package de.vinode.henne.pollbot.Background.Logic.Transport;

import java.net.InetSocketAddress;

/**
 * Created by henne on 14.03.16.
 */
public class Proxy {

    static private Proxy instance;
    private Boolean m_active = false;
    private java.net.Proxy m_proxy;

    private Proxy() {
    }

    public Proxy(String _hostname, int _port) {
        m_proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(_hostname, _port));
    }

    static public Proxy get_instance() {
        if (instance == null)
            instance = new Proxy();
        return instance;
    }

    public Boolean active() {
        return m_active;
    }

    public void set_active(Boolean _active) {
        m_active = _active;
    }


    public java.net.Proxy get_proxy() {
        return m_proxy;
    }
}
