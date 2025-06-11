package kopo.jeonnam.repository.mongo.theme;

import kopo.jeonnam.dto.theme.RecommendCoursePlanDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RecommendCoursePlanCustomRepositoryImpl implements RecommendCoursePlanCustomRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<RecommendCoursePlanDTO> findNearbyPlansWithStringCoordinates(
            double latMin, double latMax, double lngMin, double lngMax) {

        MatchOperation match = Aggregation.match(new Criteria().andOperator(
                Criteria.where("planLatitude").ne(null),
                Criteria.where("planLongitude").ne(null)
        ));

        Aggregation aggregation = Aggregation.newAggregation(
                match,
                Aggregation.project()
                        .andExpression("toDouble(planLatitude)").as("planLatitude")
                        .andExpression("toDouble(planLongitude)").as("planLongitude")
                        .andInclude(
                                "planInfoId", "planName", "planArea", "planAddr", "planPhone",
                                "planHomepage", "planParking", "planContents"
                        ),
                Aggregation.match(new Criteria().andOperator(
                        Criteria.where("planLatitude").gte(latMin).lte(latMax),
                        Criteria.where("planLongitude").gte(lngMin).lte(lngMax)
                ))
        );

        AggregationResults<RecommendCoursePlanDTO> results = mongoTemplate.aggregate(
                aggregation,
                "recommend_course_plan",
                RecommendCoursePlanDTO.class
        );

        return results.getMappedResults();
    }
}
