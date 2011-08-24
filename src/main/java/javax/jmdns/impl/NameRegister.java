/**
 *
 */
package javax.jmdns.impl;

import java.net.InetAddress;

/**
 *
 */
public interface NameRegister {

    /**
     *
     */
    public enum NameType {
        /**
         * This name represents a host name
         */
        HOST,
        /**
         * This name represents a service name
         */
        SERVICE,
    }

    public static class UniqueNamePerInterface implements NameRegister {

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#register(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public void register(InetAddress networkInterface, String name, NameType type) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#checkName(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public boolean checkName(InetAddress networkInterface, String name, NameType type) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#incrementHostName(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public String incrementHostName(InetAddress networkInterface, String name, NameType type) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static class UniqueNameAcrossInterface implements NameRegister {

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#register(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public void register(InetAddress networkInterface, String name, NameType type) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#checkName(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public boolean checkName(InetAddress networkInterface, String name, NameType type) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.NameRegister#incrementHostName(java.net.InetAddress, java.lang.String, javax.jmdns.impl.NameRegister.NameType)
         */
        @Override
        public String incrementHostName(InetAddress networkInterface, String name, NameType type) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static class Factory {

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
    public abstract void register(InetAddress networkInterface, String name, NameType type);

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
    public abstract boolean checkName(InetAddress networkInterface, String name, NameType type);

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
    public abstract String incrementHostName(InetAddress networkInterface, String name, NameType type);

}
