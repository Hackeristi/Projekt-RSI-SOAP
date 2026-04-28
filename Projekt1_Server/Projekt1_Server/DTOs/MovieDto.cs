namespace Projekt1_Server.DTOs;
public class MovieDto
{
    public int ShowId { get; set; }
    public int MovieId { get; set; }
    public string Title { get; set; }
    public string Genre { get; set; }
    public DateTime ShowDatetime { get; set; }
}