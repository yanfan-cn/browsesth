package taobe.tec.jcc;

/** Identity stub for Cimoc CopyMH/HotManga (full OpenCC optional later). */
public final class JChineseConvertor {
    private static final JChineseConvertor INSTANCE = new JChineseConvertor();

    public static JChineseConvertor getInstance() {
        return INSTANCE;
    }

    public String s2t(String s) { return s; }
    public String t2s(String s) { return s; }
}
