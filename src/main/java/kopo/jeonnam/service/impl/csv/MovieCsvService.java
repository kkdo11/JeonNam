package kopo.jeonnam.service.impl.csv;

import kopo.jeonnam.repository.entity.movie.MovieEntity;
import kopo.jeonnam.repository.mongo.movie.MovieRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieCsvService {

    @Autowired
    private MovieRepository movieRepository;

    public void importCsv(String path) throws Exception {
        Reader reader = new FileReader(path);
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader());

        List<MovieEntity> movieList = new ArrayList<>();
        for (CSVRecord record : parser) {
            MovieEntity movie = new MovieEntity();
            movie.setTitle(record.get("title"));
            movie.setLocation(record.get("planArea"));
            movie.setPosterUrl(record.get("posterURL"));
            movie .setAddr(record.get("planAddr"));

            try {
                movie.setX(Double.parseDouble(record.get("planLon")));
                movie.setY(Double.parseDouble(record.get("planLat")));
            } catch (NumberFormatException e) {
                movie.setX(0);
                movie.setY(0);
            }

            movieList.add(movie);
        }

        movieRepository.saveAll(movieList);

        parser.close();
        reader.close();
    }
}
