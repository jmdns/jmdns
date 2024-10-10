/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

        String casePreservedType = type;

        String aType = type.toLowerCase();
        String application = aType;
        String protocol = "";
        String subtype = "";
        String name = "";
        String domain = "";

        if (aType.contains("in-addr.arpa") || aType.contains("ip6.arpa")) {
            index = (aType.contains("in-addr.arpa") ? aType.indexOf("in-addr.arpa") : aType.indexOf("ip6.arpa"));
            name = ServiceInfoImpl.removeSeparators(casePreservedType.substring(0, index));
            domain = casePreservedType.substring(index);
            application = "";
        } else {
            Matcher subType = SUBTYPE_PATTERN.matcher(casePreservedType);
            if (subType.matches()) {
                name = originalCase(casePreservedType, subType, 2);
                subtype = originalCase(casePreservedType, subType, 3);
                application = originalCase(casePreservedType, subType, 4);
                protocol = originalCase(casePreservedType, subType, 5);
                domain = originalCase(casePreservedType, subType, 6);
            } else {
                Matcher normalMatcher = PATTERN.matcher(casePreservedType);
                if (normalMatcher.matches()) {
                    name = originalCase(casePreservedType, normalMatcher, 2);
                    application = originalCase(casePreservedType, normalMatcher, 3);
                    protocol = originalCase(casePreservedType, normalMatcher, 4);
                    domain = originalCase(casePreservedType, normalMatcher, 5);
                } else {
                    Matcher aTypeMatcher = TYPE_A_PATTERN.matcher(casePreservedType);
                    if (aTypeMatcher.matches()) {
                        name = originalCase(casePreservedType, aTypeMatcher, 1);
                        domain = originalCase(casePreservedType, aTypeMatcher, 2);
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
