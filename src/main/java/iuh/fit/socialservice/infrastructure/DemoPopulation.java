package iuh.fit.socialservice.infrastructure;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Cộng đồng mô phỏng cho demo: {@link #PLAYERS} người chơi và {@link #OWNERS} chủ sân hư cấu, sinh tất định theo
 * chỉ số (java.util.Random có thuật toán cố định), nên mọi service tự tính ra cùng một danh sách và cùng id
 * ({@code UUID.nameUUIDFromBytes("goatsports:user:" + email)}) mà không cần gọi nhau.
 *
 * <p><b>Bản sao giống hệt</b> ở auth-service, venue-service và social-service (mỗi service là một project Maven riêng).
 * Sửa một bản thì sửa cả ba; {@code DemoPopulationTest} ở mỗi service chốt vài giá trị để bản lệch bị phát hiện.
 */
final class DemoPopulation {
    static final int PLAYERS = 120;
    static final int OWNERS = 8;
    static final String DOMAIN = "demo.goatsports.vn";
    /** Lịch sử hoạt động mô phỏng (đặt sân, bài viết) trải từ bấy nhiêu ngày trước tới hôm nay. */
    static final int HISTORY_DAYS = 120;

    record City(String name, double latitude, double longitude, List<String> districts, int weight) { }

    static final List<City> CITIES = List.of(
            new City("Hồ Chí Minh", 10.7769, 106.7009,
                    List.of("Quận 1", "Quận 3", "Bình Thạnh", "Phú Nhuận", "Tân Bình", "Gò Vấp", "Thủ Đức", "Quận 7"), 45),
            new City("Hà Nội", 21.0278, 105.8342,
                    List.of("Cầu Giấy", "Đống Đa", "Hai Bà Trưng", "Nam Từ Liêm", "Thanh Xuân", "Long Biên"), 25),
            new City("Đà Nẵng", 16.0544, 108.2022, List.of("Hải Châu", "Sơn Trà", "Thanh Khê", "Ngũ Hành Sơn"), 15),
            new City("Khánh Hòa", 12.2388, 109.1967, List.of("Nha Trang", "Cam Ranh", "Ninh Hòa"), 15));

    /** Môn và độ phổ biến (bóng đá, cầu lông nhiều người chơi nhất). */
    static final List<String> SPORTS = List.of("FOOTBALL", "BADMINTON", "PICKLEBALL", "TENNIS", "BASKETBALL", "VOLLEYBALL");
    private static final int[] SPORT_WEIGHTS = {35, 30, 15, 10, 5, 5};
    private static final String[] SKILLS = {"BEGINNER", "INTERMEDIATE", "ADVANCED", "PRO"};
    private static final int[] SKILL_WEIGHTS = {30, 45, 20, 5};
    private static final int[] SKILL_ELO = {1000, 1250, 1500, 1750};

    private static final String[] SURNAMES = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ",
            "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý"};
    private static final String[] MALE_MIDDLE = {"Văn", "Minh", "Hoàng", "Thanh", "Quốc", "Gia", "Đức", "Bảo", "Anh",
            "Tuấn", "Hữu", "Công"};
    private static final String[] FEMALE_MIDDLE = {"Thị", "Ngọc", "Thu", "Minh", "Thanh", "Khánh", "Bảo", "Phương",
            "Hồng", "Mai"};
    private static final String[] MALE_GIVEN = {"Anh", "Bình", "Cường", "Dũng", "Duy", "Hải", "Hiếu", "Huy", "Hùng",
            "Khang", "Khoa", "Kiên", "Long", "Nam", "Nghĩa", "Phong", "Phúc", "Quang", "Sơn", "Tài", "Thắng", "Thành",
            "Tiến", "Toàn", "Trí", "Trung", "Tuấn", "Việt", "Vinh", "Vũ"};
    private static final String[] FEMALE_GIVEN = {"An", "Châu", "Chi", "Giang", "Hà", "Hân", "Hạnh", "Hương", "Lan",
            "Linh", "Mai", "My", "Ngân", "Nhi", "Như", "Phương", "Quyên", "Tâm", "Thảo", "Trâm", "Trang", "Uyên", "Vân",
            "Vy", "Yến"};

    record Slot(DayOfWeek day, LocalTime start, LocalTime end) { }

    record Player(int index, String email, String username, String fullName, boolean male, LocalDate birthDate,
                  City city, String district, double latitude, double longitude, List<String> sports, String skill,
                  int elo, double bookingsPerWeek, List<Slot> availability) {
        UUID userId() {
            return DemoPopulation.userId(email);
        }
    }

    record Owner(int index, String email, String username, String fullName, City city) {
        UUID userId() {
            return DemoPopulation.userId(email);
        }
    }

    private DemoPopulation() { }

    static UUID userId(String email) {
        return UUID.nameUUIDFromBytes(("goatsports:user:" + email).getBytes(StandardCharsets.UTF_8));
    }

    static List<Player> players() {
        List<Player> players = new ArrayList<>(PLAYERS);
        for (int index = 0; index < PLAYERS; index++) players.add(player(index));
        return players;
    }

    static Player player(int index) {
        Random random = new Random(0x60A7_5_2026L * 31 + index);
        boolean male = random.nextInt(100) < 68;
        String surname = pick(random, SURNAMES);
        String middle = male ? pick(random, MALE_MIDDLE) : pick(random, FEMALE_MIDDLE);
        String given = male ? pick(random, MALE_GIVEN) : pick(random, FEMALE_GIVEN);
        String fullName = surname + " " + middle + " " + given;
        String username = ascii(given) + "." + ascii(surname.substring(0, 1)) + ascii(middle.substring(0, 1))
                + String.format(Locale.ROOT, "%03d", index);
        City city = CITIES.get(weighted(random, CITIES.stream().mapToInt(City::weight).toArray()));
        String district = city.districts().get(random.nextInt(city.districts().size()));
        double latitude = round(city.latitude() + (random.nextDouble() - 0.5) * 0.08);
        double longitude = round(city.longitude() + (random.nextDouble() - 0.5) * 0.08);
        List<String> sports = new ArrayList<>();
        sports.add(SPORTS.get(weighted(random, SPORT_WEIGHTS)));
        if (random.nextInt(100) < 40) {
            String second = SPORTS.get(weighted(random, SPORT_WEIGHTS));
            if (!sports.contains(second)) sports.add(second);
        }
        int skill = weighted(random, SKILL_WEIGHTS);
        int elo = SKILL_ELO[skill] + random.nextInt(200);
        // Phần lớn chơi 1–2 buổi/tuần, một số ít rất năng nổ.
        double bookingsPerWeek = random.nextInt(100) < 15 ? 2.5 + random.nextDouble() * 1.5 : 0.4 + random.nextDouble() * 1.4;
        LocalDate birthDate = LocalDate.of(1985 + random.nextInt(21), 1 + random.nextInt(12), 1 + random.nextInt(28));
        return new Player(index, "sim" + String.format(Locale.ROOT, "%03d", index) + "@" + DOMAIN, username, fullName,
                male, birthDate, city, district, latitude, longitude, List.copyOf(sports), SKILLS[skill], elo,
                Math.round(bookingsPerWeek * 100) / 100.0, availability(random));
    }

    /** Giờ rảnh: tối các ngày trong tuần (sau giờ làm) và một buổi cuối tuần. */
    private static List<Slot> availability(Random random) {
        List<Slot> slots = new ArrayList<>();
        int eveningStart = 17 + random.nextInt(3);
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day.getValue() <= 5 && random.nextInt(100) < 55) {
                slots.add(new Slot(day, LocalTime.of(eveningStart, 0), LocalTime.of(Math.min(22, eveningStart + 3), 0)));
            }
        }
        DayOfWeek weekend = random.nextBoolean() ? DayOfWeek.SATURDAY : DayOfWeek.SUNDAY;
        int morning = 6 + random.nextInt(3);
        slots.add(new Slot(weekend, LocalTime.of(morning, 0), LocalTime.of(morning + 4, 0)));
        return List.copyOf(slots);
    }

    static List<Owner> owners() {
        String[][] names = {{"Trương Văn Hậu", "Hồ Chí Minh"}, {"Lâm Thị Kim Oanh", "Hồ Chí Minh"},
                {"Mai Quốc Việt", "Hồ Chí Minh"}, {"Đoàn Minh Tâm", "Hà Nội"}, {"Kiều Thị Hồng Nhung", "Hà Nội"},
                {"Tạ Đức Thịnh", "Đà Nẵng"}, {"Châu Ngọc Lan", "Khánh Hòa"}, {"Quách Văn Lực", "Khánh Hòa"}};
        List<Owner> owners = new ArrayList<>(OWNERS);
        for (int index = 0; index < OWNERS; index++) {
            String name = names[index][0];
            String cityName = names[index][1];
            City city = CITIES.stream().filter(item -> item.name().equals(cityName)).findFirst().orElseThrow();
            owners.add(new Owner(index, "simowner" + index + "@" + DOMAIN, "simowner" + index, name, city));
        }
        return owners;
    }

    /**
     * Random cho một tổ hợp khóa (ngày, người...). Seed liền nhau của java.util.Random cho số đầu tiên tương quan
     * (ngày liền kề ra quyết định giống nhau), nên trộn khóa qua splitmix64 trước.
     */
    static Random random(long... keys) {
        long hash = 0x9E3779B97F4A7C15L;
        for (long key : keys) {
            hash = (hash ^ key) * 0xBF58476D1CE4E5B9L;
            hash = (hash ^ (hash >>> 31)) * 0x94D049BB133111EBL;
            hash ^= hash >>> 29;
        }
        return new Random(hash);
    }

    static int weighted(Random random, int[] weights) {
        int total = 0;
        for (int weight : weights) total += weight;
        int roll = random.nextInt(total);
        for (int index = 0; index < weights.length; index++) {
            roll -= weights[index];
            if (roll < 0) return index;
        }
        return weights.length - 1;
    }

    private static String pick(Random random, String[] values) {
        return values[random.nextInt(values.length)];
    }

    private static double round(double value) {
        return Math.round(value * 10_000) / 10_000.0;
    }

    static String ascii(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
