package de.vinode.henne.pollbot.Background.Logic.Targets;

import android.content.Context;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import de.vinode.henne.pollbot.Background.Logic.HttpActions.Post;
import de.vinode.henne.pollbot.Background.Logic.Transport.EndPoint;
import de.vinode.henne.pollbot.Data.Grade;

/**
 * Created by henne on 14.03.16.
 */
public class Studienkonto {

    private static Studienkonto m_instance;
    private static String LOGTAG = "Studienkonto";
    static String url = "https://studienkonto.fh-erfurt.de/qisserver/rds?state=user&type=0";
    private final static String logindata = "blablubbtxt";

    private String m_login;
    private String m_password;

    private Studienkonto(Context _context) {
        readLoginData(_context);
    }

    public static Studienkonto getInstance(Context _context) {
        if (m_instance == null)
            m_instance = new Studienkonto(_context);
        return m_instance;
    }

    public LinkedHashMap<String, Grade> poll() throws IOException {
        LinkedHashMap<String, Grade> parsed_grades = new LinkedHashMap<>();
        if ( m_login == null || m_password == null) {
            Log.w(LOGTAG, "no login available");
            return parsed_grades;
        }
        HttpURLConnection uc = new EndPoint(url).get_connection();
        uc.setRequestMethod("GET");
        uc.connect();


        Document htmlDocument = Jsoup.parse(uc.getInputStream(), null, url);
        Element loginform = htmlDocument.getElementsByAttributeValue("name", "loginform").first();

        String posturl = loginform.attr("action");


        HashMap<String, String> postdata = new HashMap<>();
        postdata.put("asdf", m_login);
        postdata.put("fdsa", m_password);

        Post poster = new Post();
        htmlDocument = Jsoup.parse(poster.performPostCall(posturl, postdata));
        // Klick auf prüfungsverwaltung
        Element nextlink = htmlDocument.getElementsByAttributeValue("class", "menue").first();
        String nexturl = nextlink.getElementsContainingText("fungsverwaltung").attr("href");
        uc = new EndPoint(nexturl).get_connection();
        uc.connect();
        // Klick auf Notenspiegel
        htmlDocument = Jsoup.parse(uc.getInputStream(), null, url);
        nexturl = htmlDocument.getElementsContainingText("Notenspiegel").attr("href");
        uc = new EndPoint(nexturl).get_connection();
        uc.connect();

        htmlDocument = Jsoup.parse(uc.getInputStream(), null, url);
        nextlink = htmlDocument.getElementsByAttributeValueContaining("title", "Leistungen ").first();
        if (nextlink == null) { //Abschluss bla.. eingeklappt
            nextlink = htmlDocument.getElementsByAttributeValue("method", "POST").first().getElementsByClass("regular").first();
            nexturl = nextlink.attr("href");
            uc = new EndPoint(nexturl).get_connection();
            uc.connect();
            // Klick auf Bachelor
            htmlDocument = Jsoup.parse(uc.getInputStream(), null, url);


            nextlink = htmlDocument.getElementsByAttributeValueContaining("title", "Leistungen ").first();
        }
        nexturl = nextlink.attr("href");

        uc = new EndPoint(nexturl).get_connection();
        uc.connect();
        // Klick auf anzeigen
        htmlDocument = Jsoup.parse(uc.getInputStream(), null, url);

        Elements tables = htmlDocument.select("table");
        Element tableOfGrades = tables.iterator().next(); // just to initialize

        Iterator<Element> elementsiterator = tables.iterator();
        for (int index = 0; index < 2; ++index) { // dritte Tabelle auswählen
            tableOfGrades = elementsiterator.next();
        }

        elementsiterator = tableOfGrades.select("tr").iterator();
        for (int index = 0; index < 2; ++index) {
            elementsiterator.next(); // ab der 2. reihe
        }


        do {
            Double grade = null;
            Element row = elementsiterator.next();
            if (row.childNodeSize() == 1) {
                continue;
            }
            Iterator<Element> td_iterator = row.children().iterator();
            String text = td_iterator.next().text();
            int id = Integer.parseInt(text.replaceAll(String.valueOf((char) 160), " ").trim());
            String name = td_iterator.next().text().replaceAll(String.valueOf((char) 160), " ").trim();
            td_iterator.next(); // Art der prüfung
            try {
                grade = Double.parseDouble(td_iterator.next().text().replace(",", "."));
            } catch (Exception e) {
                // note steht noch aus
            }

            td_iterator.next(); // bestanden / nicht bestanden
            int credits = Integer.parseInt(td_iterator.next().text());
            Grade insertgrade = new Grade(name, id);
            if (grade != null) {
                insertgrade.set_grade(grade);
                insertgrade.set_credits(credits);
            }

            parsed_grades.put(name, insertgrade);
        } while (elementsiterator.hasNext());

        return parsed_grades;
    }

    private boolean readLoginData(Context _context) {
        Boolean success = false;
        try {
            FileInputStream fis = _context.openFileInput(logindata);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            m_login = reader.readLine();
            m_password = reader.readLine();
            reader.close();
            isr.close();
            fis.close();
            success = true;
            Log.d(LOGTAG, "read logindata");
        } catch (IOException e) {
            success = false;
        } catch (Exception e) {
            Log.d(LOGTAG, "error reading logindata");
        }
        return success;
    }

    private boolean writeLoginData(Context _context, String _login, String _password) {
        Boolean success = false;
        try {
            FileOutputStream fos = _context.openFileOutput(logindata, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            String separator = System.getProperty("line.separator");
            writer.write(_login + separator);
            writer.write(_password + separator);
            writer.flush();
            writer.close();
            fos.close();
            success = true;
            Log.d(LOGTAG, "login written.");
        } catch (IOException e) {
            success = false;
        }
        return success;
    }

    public void set_login(Context _context, String _login, String _password) {
        this.m_login = _login;
        this.m_password = _password;
        writeLoginData(_context, _login, _password);
    }
}