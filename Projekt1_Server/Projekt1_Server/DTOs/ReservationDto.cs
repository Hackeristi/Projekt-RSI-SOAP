namespace Projekt1_Server.Models;

public class ReservationDto
{
    public int ReservationId { get; set; }
    public string Title { get; set; }
    public DateTime ShowDatetime { get; set; }
    public List<string> TakenSeats { get; set; }
}