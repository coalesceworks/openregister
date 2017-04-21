package com.mule.test.interview.assertion;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.IgnoreTextAndAttributeValuesDifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.mule.api.MuleEvent;
import org.mule.munit.assertion.MunitAssertion;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created by sande on 21/04/2017.
 */
public class XMLAssertion implements MunitAssertion{

    private final Log logger = LogFactory.getLog(XMLAssertion.class);

    private String expected;

    public void setExpected(Resource expected) throws IOException {
        this.expected = IOUtils.toString(expected.getInputStream());
    }

    public MuleEvent execute(MuleEvent muleEvent) throws AssertionError {
        try {
                logger.info("!!!!!!!!!! expected :: "+expected);
            logger.info("(((((((((( actual :: "+muleEvent.getMessage().getPayloadAsString());
            Diff myDiff = getDiff(expected, muleEvent.getMessage().getPayloadAsString());
            if(!myDiff.similar())
            {
                throw new AssertionError("expected :: "+expected+" :: actual :: "+muleEvent.getMessage().getPayloadAsString()+" :: Differences are "+myDiff);
            }
        } catch (Exception e) {
            logger.error(e);
            throw new AssertionError(e);
        }
        return muleEvent;
    }

    private Diff getDiff(String expected, String actual) throws SAXException, IOException
    {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        DifferenceListener myDifferenceListener = new IgnoreTextAndAttributeValuesDifferenceListener();
        Diff myDiff = new Diff(expected,actual);
        myDiff.overrideDifferenceListener(myDifferenceListener);
        return myDiff;
    }
}
