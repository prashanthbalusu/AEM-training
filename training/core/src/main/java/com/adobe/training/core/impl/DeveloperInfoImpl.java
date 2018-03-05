package com.adobe.training.core.impl;

import java.util.Arrays;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.training.core.DeveloperInfo;

/**
 * Component implementation of the DeveloperInfo Service. This gets the developer info from the OSGi Configuration
 * There are 4 OSGi Configuration Examples:
 * -Boolean
 * -String
 * -String Array
 * -Dropdown
 *
 * @author Kevin Nennig (nennig@adobe.com)
 */
@Component(metatype = true, label = "Training Developer Info")
@Service(DeveloperInfo.class)
public class DeveloperInfoImpl implements DeveloperInfo{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    //Boolean Field
    @Property(label = "Show Info", description = "Should the Developer information be shown?", boolValue = false)
    public static final String PROPERTY_BOOLEAN = "show.info";
    //String Field
    @Property(label = "Name", description = "Name of the Developer")
    public static final String PROPERTY_STRING = "name";
    //String Array Field
    @Property(label = "Hobbies", description = "List your favorite Hobbies", unbounded = PropertyUnbounded.ARRAY)
    public static final String PROPERTY_ARRAY = "hobbies";
    //Dropdown Field
    private static final String OPTION_1 = "HTL";
    private static final String OPTION_2 = "Java";
    private static final String OPTION_3 = "JSP";
    private static final String OPTION_4 = "HTML";
    private static final String OPTION_5 = "JavaScript";
    @Property(label = "Language", description = "Favorite Language Preference", options={
            @PropertyOption(name=OPTION_1,value=OPTION_1),
            @PropertyOption(name=OPTION_2,value=OPTION_2),
            @PropertyOption(name=OPTION_3,value=OPTION_3),
            @PropertyOption(name=OPTION_4,value=OPTION_4),
            @PropertyOption(name=OPTION_5,value=OPTION_5)}
    )
    public static final String PROPERTY_DROPDOWN = "language.preference";

    //local variables to hold OSGi config values
    private boolean showDeveloper;
    private String developerName;
    private String[] developerHobbies;
    private String langPreference;

    @Activate
    //http://blogs.adobe.com/experiencedelivers/experience-management/osgi_activate_deactivatesignatures/
    protected void activate(Map<String, Object> properties) {
        configure(properties, "Activiated");
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        configure(properties, "Modified");
    }

    @Deactivate
    protected void deactivated(Map<String, Object> properties) {
        logger.info("#############Component (Deactivated) Good-bye " + developerName);
    }

    protected void configure(Map<String, Object> properties, String status) {
        showDeveloper = PropertiesUtil.toBoolean(properties.get(PROPERTY_BOOLEAN), false);
        developerName = PropertiesUtil.toString(properties.get(PROPERTY_STRING), "Scott Reynolds");
        developerHobbies = PropertiesUtil.toStringArray(properties.get(PROPERTY_ARRAY), new String[]{"Triangles","Circles"});
        langPreference = PropertiesUtil.toString(properties.get(PROPERTY_DROPDOWN), OPTION_1);

        logger.info("#############Component (" +status+ ") " + getDeveloperInfo());
    }

    /**
     * Method used to show how OSGi configurations can be brought into a OSGi component
     */
    public String getDeveloperInfo(){
        if(showDeveloper)
            return "Created by " + developerName
                    + ". Hobbies include: " + Arrays.toString(developerHobbies)
                    + ". Prefered programming language in AEM is: " + langPreference;
        return "";
    }

	/*
	 * Method used to show a simple OSGi service/component relationship
	public String getDeveloperInfo(){
		return "Hello! I do not know who my developer is. I am a product of random development!!!";
	}
	*/
}
