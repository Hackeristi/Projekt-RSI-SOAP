package pl.rsi.cinema;

import pl.rsi.cinema.dto.MovieFromServer;

import java.net.http.*;
import java.net.URI;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

public class CinemaServerService {

    private static final String SERVER_URL = "https://nearest-crouch-liver.ngrok-free.dev/CinemaService.asmx";

    private static CinemaServerService instance;

    public static CinemaServerService getInstance() {
        if (instance == null) {
            instance = new CinemaServerService();
        }
        return instance;
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ---------------------------
    // SOAP REQUEST HELPER
    // ---------------------------
    private String sendSoap(String action, String body) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://tempuri.org/" + action)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }

        throw new RuntimeException("SOAP error: " + response.statusCode());
    }

    // ---------------------------
    // PARSER HELPER
    // ---------------------------
    private Element parseRoot(String xml, String tag) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        return (Element) doc.getElementsByTagNameNS("*", tag).item(0);
    }

    private String getValue(Element e, String tag) {
        NodeList list = e.getElementsByTagNameNS("*", tag);

        if (list.getLength() == 0 || list.item(0) == null)
            return null;

        return list.item(0).getTextContent();
    }

    // =========================================================
    // 🎬 MOVIES LIST
    // =========================================================
    public List<MovieFromServer> getMovies() {

        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetMovies xmlns=\"http://tempuri.org/\" />" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("GetMovies", soap);

            System.out.println(xml);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            NodeList moviesNodes = doc.getElementsByTagName("d4p1:MovieDto");
            List<MovieFromServer> list = new ArrayList<>();

            for (int i = 0; i < moviesNodes.getLength(); i++) {

                Element e = (Element) moviesNodes.item(i);

                String showIdStr = getValue(e, "ShowId");

                if (showIdStr == null)
                    continue;

                int movieId = Integer.parseInt(getValue(e, "MovieId"));
                int showId = Integer.parseInt(getValue(e, "ShowId"));

                String title = getValue(e, "Title");
                String genre = getValue(e, "Genre");
                String date = getValue(e, "ShowDatetime");

                // MovieId NIE MA w tym endpointcie
                list.add(new MovieFromServer(showId, movieId, title, genre, date));
            }

            return list;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("getMovies failed: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // 🎬 MOVIE DETAILS (NAJWAŻNIEJSZE)
    // =========================================================
    public MovieDetails getMovieDetails(int movieId) {

        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetMovieDetails xmlns=\"http://tempuri.org/\">" +
                    "<movieId>" + movieId + "</movieId>" +
                    "</GetMovieDetails>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("GetMovieDetails", soap);

            Element root = parseRoot(xml, "MovieDetails");

            int duration = Integer.parseInt(getValue(root, "Duration"));

            String posterBase64 = getValue(root, "Poster");

            return new MovieDetails(
                    getValue(root, "Title"),
                    getValue(root, "Description"),
                    getValue(root, "Director"),
                    getValue(root, "Actors"),
                    duration,
                    getValue(root, "Premiere"),
                    posterBase64);

        } catch (Exception e) {
            throw new RuntimeException("getMovieDetails failed", e);
        }
    }

    // =========================================================
    // DTOs
    // =========================================================

    public static class MovieDetails {
        private final String title;
        private final String description;
        private final String director;
        private final String actors;
        private final int duration;
        private final String premiere;
        private final String posterBase64;

        public MovieDetails(String title, String description, String director,
                String actors, int duration,
                String premiere, String posterBase64) {
            this.title = title;
            this.description = description;
            this.director = director;
            this.actors = actors;
            this.duration = duration;
            this.premiere = premiere;
            this.posterBase64 = posterBase64;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getDirector() {
            return director;
        }

        public String getActors() {
            return actors;
        }

        public int getDuration() {
            return duration;
        }

        public String getPremiere() {
            return premiere;
        }

        public String getPoster() {
            return posterBase64;
        }
    }
}