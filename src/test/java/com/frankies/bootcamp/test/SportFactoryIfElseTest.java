package com.frankies.bootcamp.test;

import com.frankies.bootcamp.constant.StravaStrings.*;
import com.frankies.bootcamp.sport.BaseSport;
import com.frankies.bootcamp.sport.SportFactory;
import org.junit.jupiter.api.Test;

import static com.frankies.bootcamp.factories.StravaActivityTestFactory.dist;
import static com.frankies.bootcamp.factories.StravaActivityTestFactory.dur;
import static org.junit.jupiter.api.Assertions.*;

class SportFactoryIfElseTest {

    private static final double EPS = 1e-9;

    @Test void run_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.RUN, SportType.RUN, 10));
        assertNotNull(s);
        assertEquals(Labels.RUN, s.getSportType());
        assertEquals(10.0, s.getCalculatedDistance(), EPS);
    }

    @Test void trail_run_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.RUN, SportType.TRAIL_RUN, 10));
        assertNotNull(s);
        assertEquals(Labels.TRAIL_RUN, s.getSportType());
        assertEquals(12.0, s.getCalculatedDistance(), EPS); // 10 * 1.2
    }

    @Test void swim_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.SWIM, "Anything", 2));
        assertNotNull(s);
        assertEquals(Labels.SWIM, s.getSportType());
        assertEquals(10.0, s.getCalculatedDistance(), EPS); // 2 * 5.0
    }

    @Test void walk_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.WALK, "Whatever", 8));
        assertNotNull(s);
        assertEquals(Labels.WALK, s.getSportType());
        assertEquals(6.0, s.getCalculatedDistance(), EPS); // 8 * 0.75
    }

    @Test void hike_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.HIKE, "Whatever", 10));
        assertNotNull(s);
        assertEquals(Labels.HIKE, s.getSportType());
        assertEquals(8.5, s.getCalculatedDistance(), EPS); // 10 * 0.85
    }

    @Test void golf_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.GOLF, "Whatever", 8));
        assertNotNull(s);
        assertEquals(Labels.GOLF, s.getSportType());
        assertEquals(6.0, s.getCalculatedDistance(), EPS); // 8 * 0.75
    }

    @Test void ride_mtb_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.RIDE, SportType.MOUNTAIN_BIKE_RIDE, 20));
        assertNotNull(s);
        assertEquals(Labels.MTB, s.getSportType());
        assertEquals(10.0, s.getCalculatedDistance(), EPS); // 20 * 0.5
    }

    @Test void ride_gravel_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.RIDE, SportType.GRAVEL_RIDE, 25));
        assertNotNull(s);
        assertEquals(Labels.GRAVEL_RIDE, s.getSportType());
        assertEquals(10.0, s.getCalculatedDistance(), EPS); // 25 * 0.4
    }

    @Test void ride_road_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.RIDE, SportType.RIDE, 30));
        assertNotNull(s);
        assertEquals(Labels.ROAD_RIDE, s.getSportType());
        assertEquals(9.9, s.getCalculatedDistance(), EPS); // 30 * 0.33
    }

    @Test void virtual_ride_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.VIRTUAL_RIDE, "Any", 30));
        assertNotNull(s);
        assertEquals(Labels.VIRTUAL_RIDE, s.getSportType());
        assertEquals(9.9, s.getCalculatedDistance(), EPS); // 30 * 0.33
    }

    @Test void ebike_mtb_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.EBIKE_RIDE, SportType.E_MOUNTAIN_BIKE_RIDE, 25));
        assertNotNull(s);
        assertEquals(Labels.EBIKE_MTB, s.getSportType());
        assertEquals(10.0, s.getCalculatedDistance(), EPS); // 25 * 0.4
    }

    @Test void ebike_road_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.EBIKE_RIDE, SportType.EBIKE_RIDE, 30));
        assertNotNull(s);
        assertEquals(Labels.EBIKE_RIDE_ROAD, s.getSportType());
        assertEquals(9.9, s.getCalculatedDistance(), EPS); // 30 * 0.33
    }

    @Test void kayak_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.KAYAKING, "Any", 4));
        assertNotNull(s);
        assertEquals(Labels.KAYAK, s.getSportType());
        assertEquals(6.0, s.getCalculatedDistance(), EPS); // 4 * 1.5
    }

    @Test void soccer_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.SOCCER, "Any", 4));
        assertNotNull(s);
        assertEquals(Labels.SOCCER, s.getSportType());
        assertEquals(6.0, s.getCalculatedDistance(), EPS); // 4 * 1.5
    }

    @Test void virtual_row_mapping() {
        BaseSport s = SportFactory.getSport(dist(Type.ROWING, SportType.VIRTUAL_ROW, 10));
        assertNotNull(s);
        assertEquals(Labels.VIRTUAL_ROW, s.getSportType());
        assertEquals(10.0, s.getCalculatedDistance(), EPS); // 10 * 1.0
    }

    /* ---------- duration sports ---------- */

    @Test void surfing_mapping_duration() {
        // 7200s = 2h; Surf multiplier = 7.5 -> 15km
        BaseSport s = SportFactory.getSport(dur(Type.SURFING, SportType.SURFING, 7200));
        assertNotNull(s);
        assertEquals(Labels.SURF, s.getSportType());
        assertEquals(15.0, s.getCalculatedDistance(), EPS);
    }

    @Test void standup_paddling_duration() {
        // 3600s = 1h; SUP multiplier = 7.5 -> 7.5km
        BaseSport s = SportFactory.getSport(dur(Type.STAND_UP_PADDLING, "Any", 3600));
        assertNotNull(s);
        assertEquals(Labels.STAND_UP_PADDLING, s.getSportType());
        assertEquals(7.5, s.getCalculatedDistance(), EPS);
    }

    @Test void weight_training_duration() {
        // 3600s = 1h; WeightTraining multiplier = 5.0 -> 5.0km
        BaseSport s = SportFactory.getSport(dur(Type.WEIGHT_TRAINING, "Any", 3600));
        assertNotNull(s);
        assertEquals(Labels.WEIGHT_TRAINING, s.getSportType());
        assertEquals(5.0, s.getCalculatedDistance(), EPS);
    }

    @Test void workout_duration() {
        // 7200s = 2h; Workout multiplier = 5.0 -> 10.0km
        BaseSport s = SportFactory.getSport(dur(Type.WORKOUT, "Any", 7200));
        assertNotNull(s);
        assertEquals(Labels.WORKOUT, s.getSportType());
        assertEquals(10.0, s.getCalculatedDistance(), EPS);
    }

    /* ---------- special case + case-insensitivity ---------- */

    @Test void watersport_surfing_mapping() {
        BaseSport s = SportFactory.getSport(dur(Type.WATER_SPORT, SportType.SURFING, 3600));
        assertNotNull(s);
        assertEquals(Labels.SURF, s.getSportType());
        assertEquals(7.5, s.getCalculatedDistance(), EPS);
    }

    @Test void case_insensitive_match() {
        BaseSport s = SportFactory.getSport(dist("rIdE", "gRavElRiDe", 25));
        assertNotNull(s);
        assertEquals(Labels.GRAVEL_RIDE, s.getSportType());
        assertEquals(10.0, s.getCalculatedDistance(), EPS);
    }

    @Test void unknown_mapping_returnsNull() {
        BaseSport s = SportFactory.getSport(dist("Foobar", "Baz", 10));
        assertNull(s);
    }
}
