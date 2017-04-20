package com.mule.test.interview.util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

/**
 * Created by sande on 19/04/2017.
 */
public enum DateUtil {

    INSTANCE;

    public XMLGregorianCalendar asXMLGregorianCalendar(GregorianCalendar cal) throws DatatypeConfigurationException {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    public XMLGregorianCalendar getCurrentDateTime() throws DatatypeConfigurationException {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
    }

    public XMLGregorianCalendar asXMLGregorianCalendar(String input) throws DatatypeConfigurationException {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(javax.xml.bind.DatatypeConverter.parseDateTime(input).getTimeInMillis());
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
    }
}
