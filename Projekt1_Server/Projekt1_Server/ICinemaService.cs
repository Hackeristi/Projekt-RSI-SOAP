using Projekt1_Server.DTOs
namespace Projekt1_Server

[ServiceContract]
public interface ICinemaService
{
    [OperationContract]
    List<MovieDto> GetMovies();
    
    [OperationContract]
    MovieDetailsDto GetMOvieDetails(int movieId);
    
    [OperationContract]
    List<ShowtimeDto> GetShowtimes(int movieId);
    
    [OperationContract]
    List<SeatsDto> GetSeats(int filmshowId);

    [OperationContract]
    ReservationDto GetReservationDetails(int userId, int reservationId);

    [OperationContract]
    ReservationUpdateDto UpdateReservation(int userId, int reservationId, int newshowId, List<int> newseats);

    [OperationContract]
    ReservationCreateDto CreateReservation(int userId, int filmshowId, List<int> seats);

    [OperationContract]
    byte[] ReservationToPdf(int reservationId);
}