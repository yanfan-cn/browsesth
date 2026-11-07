package com.haleydu.cimoc.core;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.parser.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Synchronous subset of Cimoc Manga core for browsesth (SPEC-016).
 */
public final class Manga {

    public static class NetworkErrorException extends Exception {
        public NetworkErrorException() { super("network"); }
        public NetworkErrorException(String msg) { super(msg); }
    }

    public static class ParseErrorException extends Exception {
        public ParseErrorException() { super("parse"); }
    }

    private Manga() {}

    public static String getResponseBody(OkHttpClient client, Request request) throws NetworkErrorException {
        try {
            Response response = client.newCall(request).execute();
            if (response.body() == null) {
                throw new NetworkErrorException("empty body");
            }
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new NetworkErrorException("HTTP " + response.code());
            }
            return body;
        } catch (NetworkErrorException e) {
            throw e;
        } catch (IOException e) {
            throw new NetworkErrorException(e.getMessage());
        }
    }

    public static List<Chapter> getComicInfoSync(Parser parser, Comic comic) throws Exception {
        comic.setUrl(parser.getUrl(comic.getCid()));
        Request request = parser.getInfoRequest(comic.getCid());
        String html = getResponseBody(App.getHttpClient(), request);
        parser.parseInfo(html, comic);
        Request chapterReq = parser.getChapterRequest(html, comic.getCid());
        if (chapterReq != null) {
            html = getResponseBody(App.getHttpClient(), chapterReq);
        }
        Long sourceComic = Long.parseLong(comic.getSource() + "000" + (comic.getId() == null ? "00" : comic.getId()));
        List<Chapter> list = parser.parseChapter(html, comic, sourceComic);
        if (list == null) {
            list = parser.parseChapter(html);
        }
        if (list == null || list.isEmpty()) {
            throw new ParseErrorException();
        }
        return list;
    }

    public static List<ImageUrl> getChapterImageSync(Parser parser, Chapter chapter, String cid, String path) throws Exception {
        Request request = parser.getImagesRequest(cid, path);
        String html = getResponseBody(App.getHttpClient(), request);
        List<ImageUrl> list = parser.parseImages(html, chapter);
        if (list == null || list.isEmpty()) {
            list = parser.parseImages(html);
        }
        if (list == null || list.isEmpty()) {
            throw new ParseErrorException();
        }
        for (ImageUrl imageUrl : list) {
            imageUrl.setChapter(path);
        }
        return list;
    }

    public static String loadLazyUrlSync(Parser parser, String url) throws Exception {
        Request request = parser.getLazyRequest(url);
        if (request == null) {
            return url;
        }
        String html = getResponseBody(App.getHttpClient(), request);
        String out = parser.parseLazy(html, url);
        return out != null ? out : url;
    }
}
