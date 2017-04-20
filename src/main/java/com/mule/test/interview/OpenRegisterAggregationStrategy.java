package com.mule.test.interview;

import com.electrol.opr.middleware.schema.commons._0._6.ExchangeSystem;
import com.electrol.opr.middleware.schema.messageexchange._0._5.RequestHeader;
import com.electrol.opr.middleware.schema.openregister._0._1.*;
import com.mule.test.interview.util.DateUtil;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.routing.AggregationContext;
import org.mule.routing.AggregationStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by sande on 19/04/2017.
 */

public class OpenRegisterAggregationStrategy implements AggregationStrategy {
    private final Log logger = LogFactory.getLog(OpenRegisterAggregationStrategy.class);

    public MuleEvent aggregate(AggregationContext aggregationContext) throws MuleException {


        try
        {
            MuleEvent returnEvent = null;
            Map erMap = jsonToMap(aggregationContext.getOriginalEvent().getMessage().getPayloadAsString());

            if(aggregationContext.collectRouteExceptions().size() >0)
            {
                for (Integer key: aggregationContext.collectRouteExceptions().keySet()) {
                    logger.error("Exception occured for :: "+getAsString(erMap, "uid"), aggregationContext.collectRouteExceptions().get(key));
                }

                throw new DefaultMuleException(" Exception in OpenRegisterAggregationStrategy "+getAsString(erMap, "uid"));
            }

            ObjectFactory objFactory = new ObjectFactory();

            ExecuteRequest er = objFactory.createExecuteRequest();
            VoterDetails voterDetails = getVoterDetails(objFactory, erMap);

            for (MuleEvent event : aggregationContext.collectEventsWithoutExceptions()) {
                Map payload = (Map) event.getMessage().getPayload();
                if (payload.get("validateAddress") != null) {
                    for (Object address: (CopyOnWriteArrayList)payload.get("validateAddress") ) {
                        er.getResidenceAddresses().add((Address) address);
                    }
                } else if (payload.get("niCheck") != null) {
                    Map niMap = jsonToMap(aggregationContext.getOriginalEvent().getMessage().getPayloadAsString());
                    voterDetails.setNiCheckRef(getAsString(niMap,"ref"));
                    voterDetails.setIsNIValid(getAsString(niMap,"isValid") != null ?
                        Boolean.valueOf(getAsString(niMap,"isValid")) : null);
                }
                returnEvent = event;
            }

            OpenRegisterRequestType openRegisterReq = objFactory.createOpenRegisterRequestType();


            er.setVoter(voterDetails);
            er.setContactDetails(getContactDetails(objFactory, erMap));


            openRegisterReq.setRequestHeader(getRequestHeader(getAsString(erMap, "uid")));
            openRegisterReq.setRequestExecute(er);



            returnEvent.getMessage().setPayload(toXML(objFactory,openRegisterReq));


            return returnEvent;
        }catch (Exception e)
        {
            throw new DefaultMuleException("Exception occured in "+OpenRegisterAggregationStrategy.class+" due to :: ", e);
        }
    }

    private Map jsonToMap(String json) throws IOException {
        return new ObjectMapper().readValue(json, new TypeReference<Map>() {});
    }

    private RequestHeader getRequestHeader(String reqId) throws DatatypeConfigurationException {
        RequestHeader requestHeader = new RequestHeader();
        requestHeader.setRequestId(reqId);
        requestHeader.setIssueDateTime(DateUtil.INSTANCE.getCurrentDateTime());
        requestHeader.setDestination(ExchangeSystem.ORES);
        requestHeader.setSource(ExchangeSystem.DIGITAL_OR);
        return requestHeader;
    }

    private  ContactDetails getContactDetails(ObjectFactory objFactory, Map erMap)
    {
        ContactDetails contactDetails = objFactory.createContactDetails();
        contactDetails.setEmail(getAsString(erMap,"email"));
        contactDetails.setPhone(getAsString(erMap,"phone"));
        contactDetails.setPreferredContact(ContactMode.fromValue(getAsString(erMap,"contactMe")));

        return contactDetails;
    }

    private  VoterDetails getVoterDetails(ObjectFactory objFactory, Map erMap) throws DatatypeConfigurationException {
        VoterDetails voterDetails = objFactory.createVoterDetails();

        voterDetails.setFirstName(getAsString(erMap,"firstName"));
        voterDetails.setLastName(getAsString(erMap,"lastName"));
        voterDetails.setMiddleName(getAsString(erMap,"middleName"));
        voterDetails.setDob(DateUtil.INSTANCE.asXMLGregorianCalendar(getAsString(erMap,"dob")));
        voterDetails.setGender(getAsString(erMap,"gender") != null ?
                Gender.fromValue(getAsString(erMap,"gender").toUpperCase()) : null);
        voterDetails.setNationality(getAsString(erMap,"nationality"));
        voterDetails.setNi(getAsString(erMap,"ni"));
        voterDetails.setVote(getVote(objFactory, (Map)erMap.get("vote")));

        return voterDetails;
    }

    private  Vote getVote(ObjectFactory objFactory, Map _votMap) throws DatatypeConfigurationException {

        Vote vote = objFactory.createVote();
        vote.setMode(Boolean.valueOf(getAsString(_votMap,"postalVote")) ? VoteMode.POSTAL : VoteMode.INPERSON);
        vote.setPva(getAsString(_votMap,"pva") !=null ? PVA.fromValue(getAsString(_votMap,"pva")) : null);
        return vote;
    }

    private String getAsString(Map input, String key)
    {
        return input.get(key) != null ? String.valueOf(input.get(key)).trim() : null;
    }

    private String toXML(ObjectFactory objFactory, OpenRegisterRequestType openRegisterReq) throws JAXBException {
        StringWriter stringWriter = new StringWriter();
        JAXBContext.newInstance(OpenRegisterRequestType.class).createMarshaller().marshal(objFactory.createOpenRegisterRequest(openRegisterReq), stringWriter);
        return stringWriter.toString();
    }
}
