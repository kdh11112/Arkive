package arkive.admin.comm.web;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.convert.ListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;
import org.apache.commons.configuration2.reloading.ReloadingController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConfigUtil {
    private static final Log logger = LogFactory.getLog(ConfigUtil.class);
    
    private static CompositeConfiguration config;
    private static final String XML_CONFIG_FILE_NAME = "egovProps/global_common_config.xml";

    static {
        CompositeConfiguration tempConfig = new CompositeConfiguration();
        try {
            Parameters params = new Parameters();
            ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builder =
                    new ReloadingFileBasedConfigurationBuilder<>(XMLConfiguration.class)
                    .configure(params.fileBased().setFile(new File(XML_CONFIG_FILE_NAME)));

            // 리로딩 트리거를 5초 간격으로 설정
            ReloadingController reloadingController = builder.getReloadingController();
            PeriodicReloadingTrigger trigger = new PeriodicReloadingTrigger(
                    reloadingController, null, 5, TimeUnit.SECONDS);
            trigger.start();

            // XMLConfiguration 인스턴스 가져오기
            XMLConfiguration xmlConfig = builder.getConfiguration();

            // 리스트 구분자 직접 지정
            xmlConfig.setListDelimiterHandler(new DefaultListDelimiterHandler(','));

            // CompositeConfiguration에 추가
            tempConfig.addConfiguration(xmlConfig);

            logger.info("Configuration loaded and auto-reloading started for " + XML_CONFIG_FILE_NAME);
        } catch (ConfigurationException e) {
            logger.error("Failed to load configuration from " + XML_CONFIG_FILE_NAME, e);
        }
        config = tempConfig;
    }

    public static Object getProperty(String key) {
        if (config == null) {
            logger.warn("Configuration not initialized yet.");
            return null;
        }
        return config.getProperty(key);
    }
    
    /**
     * <pre>
     * property value를 String 타입으로 획득.
     * 매칭되는 property 부재 시 null 획득.
     * </pre>
     * 
     * @param String
     *            (prop) config4j.properties 파일의 property
     * @return String config4j.propertiest 파일의 해당 property value
     */
    public static String getString(String prop) {
        String value = null;
        try {
            value = config.getString(prop);
        } catch (ConversionException convException) {
        	logger.error("Configuration getString ConversionException");
        }
        return value;
    }
//  public static SubnodeConfiguration  configurationAt(String key) {
//      return config.configurationAt(key);
//  }
    /**
     * <pre>
     * property value를 String 타입으로 획득.
     * 매칭되는 property 부재 경우 대비 디폴트 value 지정 획득.
     * </pre>
     * 
     * @param String
     *            (prop) config4j.properties 파일의 property
     * @param String
     *            (defaultValue) 해당 property value 부재 시 획득할 디폴트 value
     * @return String config4j.propertiest 파일의 해당 property value
     */
    public static String getString(String prop, String defaultValue) {
        String value = null;
        try {
            value = config.getString(prop, defaultValue);
        } catch (ConversionException convException) {
        	logger.error("Configuration getString defaultValue ConversionException");
        }
        return value;
    }

    /**
     * <pre>
     * 1개의 property에 여러 value들이  쉼표로 구분되어 존재 시, 
     * 모든 value들을 String 배열로 획득.
     * 매칭되는 property 부재 시 빈 String array 획득.
     * </pre>
     * 
     * @param String
     *            (prop) config4j.properties 파일의 property
     * @return String[] config4j.propertiest 파일의 해당 property value들
     */
    public static String[] getStringArrayValue(String prop) {
        String[] value = null;
        try {
            value = config.getStringArray(prop);
        } catch (ConversionException convException) {
        	logger.error("Configuration getStringArrayValue convException");
        }
        return (value==null)?new String[0]:value;
    }
    public static String[] getStringArrayValue(String prop,ListDelimiterHandler deli) {
        String[] value = null;
        try {
            config.setListDelimiterHandler(deli);
            value = config.getStringArray(prop);
        } catch (ConversionException convException) {
        	logger.error("Configuration getStringArrayValue deli convException");
        }
        return (value==null)?new String[0]:value;
    }

    /**
     * <pre>
     * property value를 int 타입으로 획득.
     * 매칭되는 property 부재 혹은 value 타입오류 발생 시 -987654321 획득.
     * </pre>
     * 
     * @param String
     *            (prop) config4j.properties 파일의 property
     * @return int config4j.propertiest 파일의 해당 property value
     */
    public static int getIntValue(String prop) {
        int value;
        try {
            value = config.getInt(prop);
        } catch (NoSuchElementException nseException) {
            value = -987654321;
        } catch (ConversionException convException) {
            value = -987654321;
        }
        return value;
    }

    /**
     * <pre>
     * property value를 int 타입으로 획득.
     * 매칭되는 property 부재 경우 대비 디폴트 value 지정 획득.
     * property value 타입오류 발생 시 -987654321 획득.
     * </pre>
     * 
     * @param String
     *            (prop) config4j.properties 파일의 property
     * @param int (defaultValue) 해당 property value 부재 시 획득할 디폴트 value
     * @return int config4j.propertiest 파일의 해당 property value
     */
    public static int getIntValue(String prop, int defaultValue) {
        int value;
        try {
            value = config.getInt(prop, defaultValue);
        } catch (ConversionException convException) {
            value = -987654321;
        }
        return value;
    }

    /**
     * <pre>
     * property value를 float 타입으로 획득.
     * 매칭되는 property 부재 혹은 value 타입오류 발생 시 -987654321f 획득.
     * </pre>
     * 
     * @param String
     *            (prop) config4j.properties 파일의 property
     * @return float config4j.propertiest 파일의 해당 property value
     */
    public static float getFloatValue(String prop) {
        float value;
        try {
            value = config.getFloat(prop);
        } catch (NoSuchElementException nseException) {
            value = -987654321f;
        } catch (ConversionException convException) {
            value = -987654321f;
        }
        return value;
    }

    /**
     * <pre>
     * property value를 float 타입으로 획득.
     * 매칭되는 property 부재 경우 대비 디폴트 value 지정 획득.
     * property value 타입오류 발생 시 -987654321f 획득.
     * </pre>
     * 
     * @param String
     *            (prop) config4j.properties 파일의 property
     * @param float (defaultValue) 해당 property value 부재 시 획득할 디폴트 value
     * @return float config4j.propertiest 파일의 해당 property value
     */
    public static float getFloatValue(String prop, float defaultValue) {
        float value;
        try {
            value = config.getFloat(prop, defaultValue);
        } catch (ConversionException convException) {
            value = -987654321f;
        }
        return value;
    }

    /**
     * <pre>
     * property value를 boolean 타입으로 획득.
     * </pre>
     * 
     * @param String
     *            (prop) config4j.properties 파일의 property
     * @return boolean config4j.propertiest 파일의 해당 property value
     */
    public static boolean getBooleanValue(String prop) {
        return config.getBoolean(prop);
    }

    /**
     * <pre>
     * property value를 boolean 타입으로 획득.
     * 매칭되는 property 부재 경우 대비 디폴트 value 지정 획득.
     * </pre>
     * 
     * @param String
     *            (prop) config4j.properties 파일의 property
     * @param boolean (defaultValue) 해당 property value 부재 시 획득할 디폴트 value
     * @return boolean config4j.propertiest 파일의 해당 property value
     */
    public static boolean getBooleanValue(String prop, boolean defaultValue) {
        try {
            return config.getBoolean(prop, defaultValue);
        } catch (ConversionException convException) {
        	logger.error("Configuration getBooleanValue defaultValue ConversionException");
        }
        return defaultValue;
    }
}