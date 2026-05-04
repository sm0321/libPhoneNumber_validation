package generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.*;

import java.io.File;
import java.util.*;

public class DatasetGenerator {

    // =========================
    // 📦 Snapshot용 Test Case
    // =========================
    static class TestCase {
        public String input;
        public String region;
        public String caseType;

        // 🔥 actual 결과 (핵심)
        public Boolean valid;
        public String type;
        public String e164;

        public Integer countryCode;
        public Long nationalNumber;
    }

    // =========================
    // 📊 케이스 타입
    // =========================
    enum CaseType {
        VALID,
        FORMATTED,
        INVALID,
        MISMATCH,
        EDGE
    }

    // =========================
    // 🌍 국가 설정
    // =========================
    static class CountrySpec {
        final String region;
        /** 국가별로 여러 prefix 중 하나를 무작위 선택 */
        final List<String> prefixes;
        final int length;

        CountrySpec(String region, int length, List<String> prefixes) {
            this.region = region;
            this.length = length;
            this.prefixes = prefixes;
        }

        String pickPrefix(Random random) {
            return prefixes.get(random.nextInt(prefixes.size()));
        }
    }

    static final List<CountrySpec> COUNTRIES = List.of(
            new CountrySpec("KR", 8, List.of(
                    "010", "011", "016", "017", "018", "019")),
            new CountrySpec("CN", 8, List.of(
                    "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
                    "150", "151", "152", "155", "156", "185", "186", "188", "198")),
            new CountrySpec("US", 7, List.of(
                    "201", "212", "310", "415", "503", "617", "646", "702", "713", "818", "917")),
            new CountrySpec("GB", 6, List.of(
                    "7400", "7500", "7700", "7800", "7900")),
            new CountrySpec("JP", 8, List.of("70", "80", "90")),
            new CountrySpec("DE", 8, List.of(
                    "151", "152", "157", "159", "160", "162", "170", "171", "172", "173", "174", "175", "176", "177", "178")),
            new CountrySpec("FR", 7, List.of(
                    "612", "630", "650", "660", "670", "680", "690", "633")),
            new CountrySpec("CA", 7, List.of(
                    "416", "604", "613", "647", "780", "778", "236", "250", "403")),
            new CountrySpec("AU", 7, List.of(
                    "402", "403", "404", "405", "406", "407", "408", "409", "410", "412", "413", "414")),
            new CountrySpec("IN", 7, List.of(
                    "9123", "9234", "9345", "9876", "9988", "9000", "9111"))
    );

    public static void main(String[] args) throws Exception {

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        ObjectMapper mapper = new ObjectMapper();
        Random random = new Random();

        List<TestCase> dataset = new ArrayList<>();

        // 📁 저장 위치
        File dir = new File("src/test/resources");
        if (!dir.exists()) dir.mkdirs();

        // =========================
        // 🔁 데이터 생성
        // =========================
        for (int i = 0; i < 3000; i++) {

            CountrySpec country = COUNTRIES.get(random.nextInt(COUNTRIES.size()));
            CaseType type = pickCaseType(random);

            String input = generateNumber(country, type, random);
            String region = country.region;

            // 🔥 일부러 region mismatch
            if (type == CaseType.MISMATCH) {
                region = randomRegionExcept(country.region, random);
            }

            TestCase tc = new TestCase();
            tc.input = input;
            tc.region = region;
            tc.caseType = type.name();

            try {
                Phonenumber.PhoneNumber num = phoneUtil.parse(input, region);

                tc.valid = phoneUtil.isValidNumber(num);

                tc.type = phoneUtil.getNumberType(num).name();
                tc.e164 = phoneUtil.format(
                        num, PhoneNumberUtil.PhoneNumberFormat.E164);

                tc.countryCode = num.getCountryCode();
                tc.nationalNumber = num.getNationalNumber();

            } catch (Exception e) {
                tc.valid = false;
                tc.type = null;     // 🔥 여기 수정
                tc.e164 = null;
                tc.countryCode = null;
                tc.nationalNumber = null;
            }

            dataset.add(tc);
        }

        // =========================
        // 💾 파일 저장
        // =========================
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File("src/test/resources/golden-dataset.json"), dataset);

        System.out.println("✅ snapshot용 dataset 생성 완료");
    }

    // =========================
    // 🎯 케이스 분포
    // =========================
    static CaseType pickCaseType(Random random) {
        int r = random.nextInt(100);

        if (r < 80) return CaseType.VALID;
        else if (r < 85) return CaseType.FORMATTED;
        else if (r < 90) return CaseType.INVALID;
        else if (r < 95) return CaseType.MISMATCH;
        else return CaseType.EDGE;
    }

    // =========================
    // 📞 번호 생성
    // =========================
    static String generateNumber(CountrySpec country, CaseType type, Random random) {

        String prefix = country.pickPrefix(random);
        String digits = randomDigits(country.length, random);

        switch (type) {

            // MISMATCH도 동일한 국내 형식 번호를 쓰고, region만 main()에서 다른 국가로 바꿉니다.
            case VALID:
            case MISMATCH:
                return prefix + digits;

            case FORMATTED:
                return prefix + "-" +
                        digits.substring(0, 4) + "-" +
                        digits.substring(4);

            case INVALID:
                return "abc" + random.nextInt(999);

            case EDGE:
                return prefix; // 너무 짧음

            default:
                return prefix + digits;
        }
    }

    static String randomDigits(int length, Random random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    static String randomRegionExcept(String exclude, Random random) {
        List<String> regions = new ArrayList<>();
        for (CountrySpec c : COUNTRIES) {
            if (!c.region.equals(exclude)) {
                regions.add(c.region);
            }
        }
        return regions.get(random.nextInt(regions.size()));
    }
}