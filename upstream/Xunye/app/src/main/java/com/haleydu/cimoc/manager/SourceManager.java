package com.haleydu.cimoc.manager;

import android.util.SparseArray;

import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.parser.Parser;
import com.haleydu.cimoc.source.*;

/**
 * Vendored SourceManager (SPEC-016). No DB — constructs Source stubs.
 */
public class SourceManager {

    public static final int SOURCE_UNKNOW = -999;
    public static final int SOURCE_LOCAL = -2;
    /** Disabled stubs in upstream (fully commented); keep constants for compile. */
    public static final int SOURCE_EXHENTAI = 61;
    public static final int SOURCE_NHENTAI = 62;
    public static final int SOURCE_177PIC = 63;
    public static final int SOURCE_WNACG = 64;



    private static SourceManager instance;
    private final SparseArray<Parser> mParserArray = new SparseArray<>();

    public static synchronized SourceManager getInstance() {
        if (instance == null) {
            instance = new SourceManager();
        }
        return instance;
    }

    private SourceManager() {}

    private Source stub(int type, String title) {
        return new Source(null, title, type, true);
    }

    public Parser getParser(int type) {
        Parser parser = mParserArray.get(type);
        if (parser == null) {
            Source source = stub(type, "source-" + type);
            switch (type) {

                case IKanman.TYPE:
                    parser = new IKanman(source);
                    break;
                case Dmzj.TYPE:
                    parser = new Dmzj(source);
                    break;
                case HHAAZZ.TYPE:
                    parser = new HHAAZZ(source);
                    break;
                case CCTuku.TYPE:
                    parser = new CCTuku(source);
                    break;
                case U17.TYPE:
                    parser = new U17(source);
                    break;
                case DM5.TYPE:
                    parser = new DM5(source);
                    break;
                case Webtoon.TYPE:
                    parser = new Webtoon(source);
                    break;
                case MH57.TYPE:
                    parser = new MH57(source);
                    break;
                case MH50.TYPE:
                    parser = new MH50(source);
                    break;
                case Dmzjv2.TYPE:
                    parser = new Dmzjv2(source);
                    break;
                case MangaNel.TYPE:
                    parser = new MangaNel(source);
                    break;

                case PuFei.TYPE:
                    parser = new PuFei(source);
                    break;
                case Tencent.TYPE:
                    parser = new Tencent(source);
                    break;
                case BuKa.TYPE:
                    parser = new BuKa(source);
                    break;
                case EHentai.TYPE:
                    parser = new EHentai(source);
                    break;
                case QiManWu.TYPE:
                    parser = new QiManWu(source);
                    break;
                case Hhxxee.TYPE:
                    parser = new Hhxxee(source);
                    break;
                case Cartoonmad.TYPE:
                    parser = new Cartoonmad(source);
                    break;
                case Animx2.TYPE:
                    parser = new Animx2(source);
                    break;
                case MH517.TYPE:
                    parser = new MH517(source);
                    break;
                case MiGu.TYPE:
                    parser = new MiGu(source);
                    break;
                case BaiNian.TYPE:
                    parser = new BaiNian(source);
                    break;
                case ChuiXue.TYPE:
                    parser = new ChuiXue(source);
                    break;
                case TuHao.TYPE:
                    parser = new TuHao(source);
                    break;
                case SixMH.TYPE:
                    parser = new SixMH(source);
                    break;
                case ManHuaDB.TYPE:
                    parser = new ManHuaDB(source);
                    break;
                case Manhuatai.TYPE:
                    parser = new Manhuatai(source);
                    break;
                case GuFeng.TYPE:
                    parser = new GuFeng(source);
                    break;
                case CCMH.TYPE:
                    parser = new CCMH(source);
                    break;
                case MHLove.TYPE:
                    parser = new MHLove(source);
                    break;
                case YYLS.TYPE:
                    parser = new YYLS(source);
                    break;
                case JMTT.TYPE:
                    parser = new JMTT(source);
                    break;

                case Mangakakalot.TYPE:
                    parser = new Mangakakalot(source);
                    break;
                case Ohmanhua.TYPE:
                    parser = new Ohmanhua(source);
                    break;
                case CopyMH.TYPE:
                    parser = new CopyMH(source);
                    break;
                case HotManga.TYPE:
                    parser = new HotManga(source);
                    break;
                case MangaBZ.TYPE:
                    parser = new MangaBZ(source);
                    break;
                case WebtoonDongManManHua.TYPE:
                    parser = new WebtoonDongManManHua(source);
                    break;
                case MH160.TYPE:
                    parser = new MH160(source);
                    break;
                case QiMiaoMH.TYPE:
                    parser = new QiMiaoMH(source);
                    break;
                case YKMH.TYPE:
                    parser = new YKMH(source);
                    break;
                case DmzjFix.TYPE:
                    parser = new DmzjFix(source);
                    break;
                default:
                    parser = new Null();
                    break;
            }
            mParserArray.put(type, parser);
        }
        return parser;
    }

    /** All registered web parser types (excludes Locality). */
    public static int[] allWebTypes() {
        return new int[] {
            IKanman.TYPE,
            Dmzj.TYPE,
            HHAAZZ.TYPE,
            CCTuku.TYPE,
            U17.TYPE,
            DM5.TYPE,
            Webtoon.TYPE,
            MH57.TYPE,
            MH50.TYPE,
            Dmzjv2.TYPE,
            MangaNel.TYPE,
            PuFei.TYPE,
            Tencent.TYPE,
            BuKa.TYPE,
            EHentai.TYPE,
            QiManWu.TYPE,
            Hhxxee.TYPE,
            Cartoonmad.TYPE,
            Animx2.TYPE,
            MH517.TYPE,
            MiGu.TYPE,
            BaiNian.TYPE,
            ChuiXue.TYPE,
            TuHao.TYPE,
            SixMH.TYPE,
            ManHuaDB.TYPE,
            Manhuatai.TYPE,
            GuFeng.TYPE,
            CCMH.TYPE,
            MHLove.TYPE,
            YYLS.TYPE,
            JMTT.TYPE,
            Mangakakalot.TYPE,
            Ohmanhua.TYPE,
            CopyMH.TYPE,
            HotManga.TYPE,
            MangaBZ.TYPE,
            WebtoonDongManManHua.TYPE,
            MH160.TYPE,
            QiMiaoMH.TYPE,
            YKMH.TYPE,
            DmzjFix.TYPE,
        };
    }
}
