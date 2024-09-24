package javax.jmdns.impl;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.jmdns.ServiceInfo.Fields;

class ServiceTypeDecoder {

    private static final Pattern SUBTYPE_PATTERN = Pattern.compile("^((.*)\\._)?_?(.*)\\._sub\\._([^.]*)\\._([^.]*)\\.(.*)\\.?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN = Pattern.compile("^((.*)?\\._)?([^.]*)\\._([^.]*)\\.(.*)\\.?$");

    private static final Pattern TYPE_A_PATTERN = Pattern.compile("^([^.]*)\\.(.*)\\.?$");

    private ServiceTypeDecoder() {
    }

    static Map<Fields, String> decodeQualifiedNameMap(String type, String name, String subtype) {
        Map<Fields, String> qualifiedNameMap = decodeQualifiedNameMapForType(type);

        qualifiedNameMap.put(Fields.Instance, name);
        qualifiedNameMap.put(Fields.Subtype, subtype);

        return ServiceInfoImpl.checkQualifiedNameMap(qualifiedNameMap);
    }

    static Map<Fields, String> decodeQualifiedNameMapForType(String type) {
        int index;

        String aType = type.toLowerCase();
        String application = aType;
        String protocol = "";
        String subtype = "";
        String name = "";
        String domain = "";

        if (aType.contains("in-addr.arpa") || aType.contains("ip6.arpa")) {
            index = (aType.contains("in-addr.arpa") ? aType.indexOf("in-addr.arpa") : aType.indexOf("ip6.arpa"));
            name = ServiceInfoImpl.removeSeparators(type.substring(0, index));
            domain = type.substring(index);
            application = "";
        } else {
            Matcher subType = SUBTYPE_PATTERN.matcher(type);
            if (subType.matches()) {
                name = originalCase(type, subType, 2);
                subtype = originalCase(type, subType, 3);
                application = originalCase(type, subType, 4);
                protocol = originalCase(type, subType, 5);
                domain = originalCase(type, subType, 6);
            } else {
                Matcher normalMatcher = PATTERN.matcher(type);
                if (normalMatcher.matches()) {
                    name = originalCase(type, normalMatcher, 2);
                    application = originalCase(type, normalMatcher, 3);
                    protocol = originalCase(type, normalMatcher, 4);
                    domain = originalCase(type, normalMatcher, 5);
                } else {
                    Matcher aTypeMatcher = TYPE_A_PATTERN.matcher(type);
                    if (aTypeMatcher.matches()) {
                        name = originalCase(type, aTypeMatcher, 1);
                        domain = originalCase(type, aTypeMatcher, 2);
                        application = "";
                    }
                }
            }
        }

        return ServiceInfoImpl.createQualifiedMap(name, ServiceInfoImpl.removeSeparators(application), protocol, ServiceInfoImpl.removeSeparators(domain), subtype);
    }


    private static String originalCase(String casePreservedType, Matcher matcher, int group) {
        if (matcher.start(group) != -1) {
            return casePreservedType.substring(matcher.start(group), matcher.end(group));
        }
        return "";
    }
}