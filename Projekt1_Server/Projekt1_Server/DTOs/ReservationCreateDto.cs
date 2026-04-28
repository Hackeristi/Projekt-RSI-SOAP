namespace Projekt1_Server.Models;

public class ReservationCreateDto
{
    public int UsersId { get; set; }
    public int FilmShowId { get; set; }
    public List<int> SelectedSeats { get; set; }
}