namespace Projekt1_Server.Models;

public class MovieDetailsDto
{
    public string Title { get; set; }
    public string Director { get; set; }
    public string Description { get; set; }
    public int Premiere { get; set; }
    public int Duration { get; set; }
    public byte[] Poster { get; set; }
    public virtual ICollection<Actor> Actors { get; set; } = new List<Actor>();
}