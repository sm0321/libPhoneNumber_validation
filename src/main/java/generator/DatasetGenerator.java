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
        String region;
        String prefix;
        int length;

        CountrySpec(String region, String prefix, int length) {
            this.region = region;
            this.prefix = prefix;
            this.length = length;
        }
    }

    static final List<CountrySpec> COUNTRIES = List.of(
            new CountrySpec("KR", "010", 8),
            new CountrySpec("US", "201", 7),
            new CountrySpec("GB", "7700", 6),
            new CountrySpec("JP", "90", 8),
            new CountrySpec("DE", "151", 8),
            new CountrySpec("FR", "612", 7),
            new CountrySpec("CA", "416", 7),
            new CountrySpec("AU", "412", 7),
            new CountrySpec("IN", "9123", 7)
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
        for (int i = 0; i < 300; i++) {

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

        if (r < 50) return CaseType.VALID;
        else if (r < 70) return CaseType.FORMATTED;
        else if (r < 85) return CaseType.INVALID;
        else if (r < 95) return CaseType.MISMATCH;
        else return CaseType.EDGE;
    }

    // =========================
    // 📞 번호 생성
    // =========================
    static String generateNumber(CountrySpec country, CaseType type, Random random) {

        String digits = randomDigits(country.length, random);

        switch (type) {

            case VALID:
                return country.prefix + digits;

            case FORMATTED:
                return country.prefix + "-" +
                        digits.substring(0, 4) + "-" +
                        digits.substring(4);

            case INVALID:
                return "abc" + random.nextInt(999);

            case MISMATCH:
                return country.prefix + digits;

            case EDGE:
                return country.prefix; // 너무 짧음

            default:
                return country.prefix + digits;
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