/**
 *
 */
package javax.jmdns.impl;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public interface NameRegister {

    /**
     *
     */
    enum NameType {
        /**
         * This name represents a host name
         */
        HOST,
        /**
         * This name represents a service name
         */
        SERVICE,
    }

    abstract class BaseRegister implements NameRegister {

        protected String incrementNameWithDash(String name) {
            final StringBuilder givenName = new StringBuilder(name.length() + 5);
            int hostNameCount;
            int plocal = name.indexOf(".local.");
            int punder = name.lastIndexOf('-');
            if (punder < 0) {
                // This is the first increment
                hostNameCount = 1;
                givenName.append(name, 0, plocal);
            } else {
                try {
                    int value = Integer.parseInt(name.substring(punder + 1, plocal));
                    hostNameCount = value + 1;
                    givenName.append(name, 0, punder);
                } catch (Exception e) {
                    // If we got an exception this means that we have a name with a "-"
                    hostNameCount = 1;
                    givenName.append(name, 0, plocal);
                }
            }
            givenName.append('-');
            givenName.append(hostNameCount);
            givenName.append(".local.");
            return givenName.toString();
        }

        protected String incrementNameWithParentesis(String name) {
            StringBuilder givenName = new StringBuilder(name.length() + 5);
            final int l = name.lastIndexOf('(');
            final int r = name.lastIndexOf(')');
            if ((l >= 0) && (l < r)) {
                try {
                    givenName.append(name, 0, l);
                    givenName.append('(');
                    givenName.append(Integer.parseInt(name.substring(l + 1, r)) + 1);
                    givenName.append(')');
                } catch (final NumberFormatException e) {
                    givenName.setLength(0);
                    givenName.append(name);
                    givenName.append(" (2)");
                }
            } else {
                givenName.append(name);
                givenName.append(" (2)");
            }
            return givenName.toString();
        }

    }

    class UniqueNamePerInterface extends BaseRegister {

        private final ConcurrentMap<InetAddress, String>      _hostNames;
        private final ConcurrentMap<InetAddress, Set<String>> _serviceNames;

        public UniqueNamePerInterface() {
            super();
            _hostNames = new ConcurrentHashMap<>();
            _serviceNames = new ConcurrentHashMap<>();
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#register(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public void register(InetAddress networkInterface, String name, NameType type) {
            switch (type) {
                case HOST, SERVICE:
                    break;
                default:
                    // this is trash to keep the compiler happy
            }
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#checkName(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public boolean checkName(InetAddress networkInterface, String name, NameType type) {
            return switch (type) {
                case HOST -> {
                    String hostname = _hostNames.get(networkInterface);
                    yield hostname != null && hostname.equals(name);
                }
                case SERVICE -> {
                    Set<String> names = _serviceNames.get(networkInterface);
                    yield names != null && names.contains(name);
                }
                // this is trash to keep the compiler happy
            };
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#incrementHostName(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public String incrementName(InetAddress networkInterface, String name, NameType type) {
            return switch (type) {
                case HOST -> this.incrementNameWithDash(name);
                case SERVICE -> this.incrementNameWithParentesis(name);
                // this is trash to keep the compiler happy
            };
        }

    }

    class UniqueNameAcrossInterface extends BaseRegister {

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#register(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public void register(InetAddress networkInterface, String name, NameType type) {
            switch (type) {
                case HOST, SERVICE:
                    break;
                default:
                    // this is trash to keep the compiler happy
            }
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#checkName(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public boolean checkName(InetAddress networkInterface, String name, NameType type) {
            // this is trash to keep the compiler happy
            return false;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#incrementHostName(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public String incrementName(InetAddress networkInterface, String name, NameType type) {
            return switch (type) {
                case HOST -> this.incrementNameWithDash(name);
                case SERVICE -> this.incrementNameWithParentesis(name);
                // this is trash to keep the compiler happy
            };
        }

    }

    class Factory {

        private static volatile NameRegister _register;

        /**
         * Register a Name register.
         *
         * @param register
         *            new register
         * @throws IllegalStateException
         *             the register can only be set once
         */
        public static void setRegistry(NameRegister register) throws IllegalStateException {
            if (_register != null) {
                throw new IllegalStateException("The register can only be set once.");
            }
            if (register != null) {
                _register = register;
            }
        }

        /**
         * Returns the name register.
         *
         * @return name register
         */
        public static NameRegister getRegistry() {
            if (_register == null) {
                _register = new UniqueNamePerInterface();
            }
            return _register;
        }

    }

    /**
     * Registers a name that is defended by this group of mDNS.
     *
     * @param networkInterface
     *            IP address to handle
     * @param name
     *            name to register
     * @param type
     *            name type to register
     */
    void register(InetAddress networkInterface, String name, NameType type);

    /**
     * Checks a name that is defended by this group of mDNS.
     *
     * @param networkInterface
     *            IP address to handle
     * @param name
     *            name to check
     * @param type
     *            name type to check
     * @return <code>true</code> if the name is not in conflict, <code>flase</code> otherwise.
     */
    boolean checkName(InetAddress networkInterface, String name, NameType type);

    /**
     * Increments a name that is defended by this group of mDNS after it has been found in conflict.
     *
     * @param networkInterface
     *            IP address to handle
     * @param name
     *            name to increment
     * @param type
     *            name type to increments
     * @return new name
     */
    String incrementName(InetAddress networkInterface, String name, NameType type);

}