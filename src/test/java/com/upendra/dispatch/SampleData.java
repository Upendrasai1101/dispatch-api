package com.upendra.dispatch;

import com.upendra.dispatch.domain.Order;
import com.upendra.dispatch.domain.Priority;
import com.upendra.dispatch.domain.Vehicle;

import java.util.List;

/** The 30 sample orders and 5 sample vehicles used for the demo. */
public final class SampleData {

    public static final double CP_LAT = 28.6139, CP_LON = 77.2090;
    public static final double KB_LAT = 28.7041, KB_LON = 77.1025;
    public static final double NO_LAT = 28.5355, NO_LON = 77.3910;
    public static final double GU_LAT = 28.4595, GU_LON = 77.0266;

    private static final String CP_ADDR = "Connaught Place, Delhi, India";
    private static final String KB_ADDR = "Karol Bagh, Delhi, India";
    private static final String NO_ADDR = "Sector 18, Noida, Uttar Pradesh, India";
    private static final String GU_ADDR = "Cyber Hub, Gurgaon, Haryana, India";

    private SampleData() {
    }

    public static List<Order> orders() {
        return List.of(
                new Order("ORD001", CP_LAT, CP_LON, CP_ADDR, 15, Priority.HIGH),
                new Order("ORD002", CP_LAT, CP_LON, CP_ADDR, 10, Priority.MEDIUM),
                new Order("ORD003", KB_LAT, KB_LON, KB_ADDR, 20, Priority.LOW),
                new Order("ORD004", NO_LAT, NO_LON, NO_ADDR, 25, Priority.HIGH),
                new Order("ORD005", GU_LAT, GU_LON, GU_ADDR, 30, Priority.MEDIUM),
                new Order("ORD006", CP_LAT, CP_LON, CP_ADDR, 40, Priority.LOW),
                new Order("ORD007", KB_LAT, KB_LON, KB_ADDR, 20, Priority.HIGH),
                new Order("ORD008", NO_LAT, NO_LON, NO_ADDR, 25, Priority.HIGH),
                new Order("ORD009", GU_LAT, GU_LON, GU_ADDR, 15, Priority.MEDIUM),
                new Order("ORD010", CP_LAT, CP_LON, CP_ADDR, 30, Priority.LOW),
                new Order("ORD011", KB_LAT, KB_LON, KB_ADDR, 20, Priority.HIGH),
                new Order("ORD012", NO_LAT, NO_LON, NO_ADDR, 10, Priority.MEDIUM),
                new Order("ORD013", GU_LAT, GU_LON, GU_ADDR, 25, Priority.HIGH),
                new Order("ORD014", CP_LAT, CP_LON, CP_ADDR, 15, Priority.LOW),
                new Order("ORD015", KB_LAT, KB_LON, KB_ADDR, 20, Priority.HIGH),
                new Order("ORD016", NO_LAT, NO_LON, NO_ADDR, 30, Priority.LOW),
                new Order("ORD017", GU_LAT, GU_LON, GU_ADDR, 15, Priority.MEDIUM),
                new Order("ORD018", CP_LAT, CP_LON, CP_ADDR, 10, Priority.HIGH),
                new Order("ORD019", KB_LAT, KB_LON, KB_ADDR, 20, Priority.LOW),
                new Order("ORD020", NO_LAT, NO_LON, NO_ADDR, 30, Priority.HIGH),
                new Order("ORD021", CP_LAT, CP_LON, CP_ADDR, 25, Priority.MEDIUM),
                new Order("ORD022", KB_LAT, KB_LON, KB_ADDR, 20, Priority.LOW),
                new Order("ORD023", NO_LAT, NO_LON, NO_ADDR, 30, Priority.HIGH),
                new Order("ORD024", GU_LAT, GU_LON, GU_ADDR, 15, Priority.LOW),
                new Order("ORD025", CP_LAT, CP_LON, CP_ADDR, 20, Priority.MEDIUM),
                new Order("ORD026", KB_LAT, KB_LON, KB_ADDR, 25, Priority.HIGH),
                new Order("ORD027", NO_LAT, NO_LON, NO_ADDR, 20, Priority.MEDIUM),
                new Order("ORD028", GU_LAT, GU_LON, GU_ADDR, 15, Priority.LOW),
                new Order("ORD029", CP_LAT, CP_LON, CP_ADDR, 30, Priority.HIGH),
                new Order("ORD030", KB_LAT, KB_LON, KB_ADDR, 20, Priority.LOW));
    }

    public static List<Vehicle> vehicles() {
        return List.of(
                new Vehicle("VEH001", 100, KB_LAT, KB_LON, KB_ADDR),
                new Vehicle("VEH002", 80, NO_LAT, NO_LON, NO_ADDR),
                new Vehicle("VEH003", 120, GU_LAT, GU_LON, GU_ADDR),
                new Vehicle("VEH004", 90, CP_LAT, CP_LON, CP_ADDR),
                new Vehicle("VEH005", 110, KB_LAT, KB_LON, KB_ADDR));
    }
}
