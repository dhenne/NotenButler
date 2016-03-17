package de.vinode.henne.pollbot.Data;

/**
 * Created by henne on 14.03.16.
 */
public class Grade {

    private double m_grade;
    private String m_name;
    private int m_credits;
    private int m_id;

    public Grade(String _name, int _id) {
        this.m_name = _name;
        this.m_id = _id;
    }

    public double grade() {
        return m_grade;
    }

    public void set_grade(double m_grade) {
        this.m_grade = m_grade;
    }

    public String name() {
        return m_name;
    }

    public void set_name(String m_name) {
        this.m_name = m_name;
    }

    public int credits() {
        return m_credits;
    }

    public void set_credits(int m_credits) {
        this.m_credits = m_credits;
    }

    public int id() {
        return m_id;
    }

    public void set_id(int m_id) {
        this.m_id = m_id;
    }

    @Override
    public String toString() {
        return "< '" + m_name + "' -> " + m_grade + " >";
    }
}
