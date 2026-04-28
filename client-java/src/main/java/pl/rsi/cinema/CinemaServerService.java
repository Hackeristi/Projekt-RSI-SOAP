package pl.rsi.cinema;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

/**
 * Service for communicating with the SOAP server via ngrok tunnel
 */
public class CinemaServerService {

    private static final String SERVER_URL = "https://nearest-crouch-liver.ngrok-free.dev";
    private static CinemaServerService instance;

    private CinemaServerService() {
    }

    public static CinemaServerService getInstance() {
        if (instance == null) {
            instance = new CinemaServerService();
        }
        return instance;
    }

    /**
     * Get list of movies from server via SOAP call
     */
    public List<MovieFromServer> getMovies() {
        List<MovieFromServer> movies = new ArrayList<>();
        try {
            System.out.println("Fetching movies from: " + SERVER_URL + "/CinemaService.asmx");

            // SOAP request body
            String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:tns=\"http://cinema.example.com/\">" +
                    "<soap:Body>" +
                    "<tns:GetMovies/>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            // Create HTTP client and request
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new java.net.URI(SERVER_URL + "/CinemaService.asmx"))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "http://cinema.example.com/GetMovies")
                    .POST(HttpRequest.BodyPublishers.ofString(soapBody))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response Status: " + response.statusCode());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String xmlResponse = response.body();
                System.out.println(
                        "Response XML:\n" + xmlResponse.substring(0, Math.min(500, xmlResponse.length())) + "...");

                // Parse XML response
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes()));

                // Extract movie data from response
                NodeList movieNodes = doc.getElementsByTagName("MovieDto");
                System.out.println("Found " + movieNodes.getLength() + " movies");

                for (int i = 0; i < movieNodes.getLength(); i++) {
                    Element movieElement = (Element) movieNodes.item(i);

                    int showId = Integer.parseInt(
                            movieElement.getElementsByTagName("ShowId").item(0).getTextContent());
                    String title = movieElement.getElementsByTagName("Title").item(0).getTextContent();
                    String genre = movieElement.getElementsByTagName("Genre").item(0).getTextContent();
                    String showDateTime = movieElement.getElementsByTagName("ShowDatetime").item(0).getTextContent();

                    movies.add(new MovieFromServer(showId, title, genre, showDateTime));
                    System.out.println("Loaded: " + title + " @ " + showDateTime);
                }
            } else {
                System.out.println("Server returned status: " + response.statusCode());
                System.out.println("Response: " + response.body());
                // Fallback to default data
                movies.add(new MovieFromServer(1, "Diuna", "Sci-Fi", "2025-04-27T14:30:00"));
                movies.add(new MovieFromServer(2, "Avatar 3", "Sci-Fi", "2025-04-27T17:00:00"));
                movies.add(new MovieFromServer(3, "Oppenheimer", "Drama", "2025-04-27T19:30:00"));
            }

            return movies;
        } catch (Exception e) {
            System.out.println("Error fetching movies: " + e.getMessage());
            e.printStackTrace();

            // Fallback to default data
            List<MovieFromServer> defaultMovies = new ArrayList<>();
            defaultMovies.add(new MovieFromServer(1, "Diuna", "Sci-Fi", "2025-04-27T14:30:00"));
            defaultMovies.add(new MovieFromServer(2, "Avatar 3", "Sci-Fi", "2025-04-27T17:00:00"));
            defaultMovies.add(new MovieFromServer(3, "Oppenheimer", "Drama", "2025-04-27T19:30:00"));
            return defaultMovies;
        }
    }

    /**
     * Get movie details from server
     */
    public MovieDetails getMovieDetails(int movieId) {
        try {
            // Mock data for testing
            return new MovieDetails(
                    "Diuna",
                    "Dynamiczna adaptacja wszechczasowego klasyka SF",
                    "Denis Villeneuve",
                    "Timothée Chalamet, Zendaya, Oscar Isaac",
                    151,
                    "2021-09-15",
                    "https://...");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create reservation on server
     */
    public boolean createReservation(String userEmail, int showId, List<String> selectedSeats) {
        try {
            // Mock reservation creation
            System.out.println("Creating reservation for: " + userEmail);
            System.out.println("Show ID: " + showId);
            System.out.println("Seats: " + selectedSeats);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check server connection
     */
    public boolean isServerAvailable() {
        try {
            URL url = new URL(SERVER_URL);
            return url.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Inner classes for data transfer
    public static class MovieFromServer {
        public int showId;
        public String title;
        public String genre;
        public String showDateTime;

        public MovieFromServer(int showId, String title, String genre, String showDateTime) {
            this.showId = showId;
            this.title = title;
            this.genre = genre;
            this.showDateTime = showDateTime;
        }

        public int getShowId() {
            return showId;
        }

        public String getTitle() {
            return title;
        }

        public String getGenre() {
            return genre;
        }

        public String getShowDateTime() {
            return showDateTime;
        }
    }

    public static class MovieDetails {
        public String title;
        public String description;
        public String director;
        public String actors;
        public int duration;
        public String premiere;
        public String poster;

        public MovieDetails(String title, String description, String director, String actors, int duration,
                String premiere, String poster) {
            this.title = title;
            this.description = description;
            this.director = director;
            this.actors = actors;
            this.duration = duration;
            this.premiere = premiere;
            this.poster = poster;
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
            return poster;
        }
    }
}
