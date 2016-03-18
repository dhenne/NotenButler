package de.vinode.henne.pollbot.Data;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Created by henne on 14.03.16.
 */
public class Environment {

    private LinkedHashMap<String, Grade> PERSISTENT_LIST;
    private final static String data_store_filename = "grades.txt";
    private Charset charset;
    private static Environment m_instance;


    private static final String LOGTAG = "Environment";

    public boolean readPersistentData(Context _context) {
        Boolean success = false;
        try {
            FileInputStream fis = _context.openFileInput(data_store_filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] args = line.trim().split(";");
                Grade readgrade = new Grade(args[1], Integer.parseInt(args[0]));
                readgrade.set_grade(Double.parseDouble(args[2]));
                readgrade.set_credits(Integer.parseInt(args[3]));
                PERSISTENT_LIST.put(readgrade.name(), readgrade);
                Log.d(LOGTAG, "reading from config: " + readgrade);
            }
            reader.close();
            isr.close();
            fis.close();
            success = true;
        } catch (IOException e) {
            success = false;
        }
        return success;
    }

    public boolean writePersistentData(Context _context) {
        Boolean success = false;
        try {
            FileOutputStream fos = _context.openFileOutput(data_store_filename, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            String separator = System.getProperty("line.separator");
            for (Map.Entry<String, Grade> entrygrade : PERSISTENT_LIST.entrySet()) {
                Grade grade = entrygrade.getValue();
                writer.write(grade.id() + ";" + grade.name() + ";" + grade.grade() + ";" + grade.credits() + separator);
                Log.d(LOGTAG, "writing to store: " + grade);
            }
            writer.flush();
            writer.close();
            fos.close();
            success = true;
        } catch (IOException e) {
            success = false;
        }
        return success;
    }

    public LinkedHashSet<String> verifyPersistentData(Context _context, LinkedHashMap<String, Grade> _remote_list) {
        Boolean needs_update = false;
        LinkedHashSet<String> found = new LinkedHashSet<>();
        for (Map.Entry<String, Grade> remote_grade_entry : _remote_list.entrySet()) {
            if (    (!PERSISTENT_LIST.containsKey(remote_grade_entry.getKey()) && (remote_grade_entry.getValue().grade() != 0.0)) ||
                    (PERSISTENT_LIST.containsKey(remote_grade_entry.getKey()) &&
                            PERSISTENT_LIST.get(remote_grade_entry.getKey()).grade() != remote_grade_entry.getValue().grade())
                    ) {
                needs_update = true;
                PERSISTENT_LIST.put(remote_grade_entry.getKey(), remote_grade_entry.getValue());
                found.add(remote_grade_entry.getKey());
            }
        }

        if (needs_update) {
            writePersistentData(_context);
        }

        return found;
    }

    public static Environment getInstance() {
        if (m_instance == null)
            m_instance = new Environment();
        return m_instance;
    }

    private Environment() {
        PERSISTENT_LIST = new java.util.LinkedHashMap<>();
        Charset.defaultCharset();
    }

    public LinkedHashMap<String, Grade> PERSISTENT_LIST() {
        return PERSISTENT_LIST;
    }

    public void deleteGrades(Context _context) {
        File file = new File(_context.getFilesDir() + "/" + data_store_filename);
        if (file.delete()) {
            PERSISTENT_LIST.clear();
            Log.i(LOGTAG, "deleted saved grades");
        } else {
            Log.e(LOGTAG, "error deleting grades");
        }
    }
}
