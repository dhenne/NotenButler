package de.vinode.henne.pollbot.Background.Logic.Transport;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by henne on 14.03.16.
 */
public class EndPoint {

    HttpURLConnection m_uc;

    public EndPoint(String _url) throws IOException {

        URL url_for_proxy = new URL(_url);

        if (Proxy.get_instance().active()) {
            java.net.Proxy proxy = Proxy.get_instance().get_proxy();
            m_uc = (HttpURLConnection) url_for_proxy.openConnection(proxy);
        } else {
            m_uc = (HttpURLConnection) url_for_proxy.openConnection();
        }

        m_uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        m_uc.setRequestProperty("Content-Language", "de");

    }

    public HttpURLConnection get_connection() {
        return m_uc;
    }
}
