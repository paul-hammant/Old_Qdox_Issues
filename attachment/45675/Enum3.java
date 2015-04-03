
package vigateway;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Enum-3.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Enum-3">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="UserPersmissions-Monitor"/>
 *     &lt;enumeration value="UserPersmissions-Setup"/>
 *     &lt;enumeration value="UserPersmissions-Config"/>
 *     &lt;enumeration value="UserPersmissions-All"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "Enum-3")
@XmlEnum
public enum Enum3 {

    @XmlEnumValue("UserPersmissions-Monitor")
    USER_PERSMISSIONS_MONITOR("UserPersmissions-Monitor"),
    @XmlEnumValue("UserPersmissions-Setup")
    USER_PERSMISSIONS_SETUP("UserPersmissions-Setup"),
    @XmlEnumValue("UserPersmissions-Config")
    USER_PERSMISSIONS_CONFIG("UserPersmissions-Config"),
    @XmlEnumValue("UserPersmissions-All")
    USER_PERSMISSIONS_ALL("UserPersmissions-All");
    private final String value;

    Enum3(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Enum3 fromValue(String v) {
        for (Enum3 c: Enum3 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
