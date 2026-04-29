package generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.*;

import java.io.File;
import java.util.*;

public class DatasetGenerator {

    static class TestCase {
        public String input;
        public String region;
        public boolean expectedValid;
        public String expectedType;
        public String expectedE164;
    }

    // 🌍 국가별 대표 prefix + 샘플 번호 생성 규칙
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

        List<TestCase> dataset = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 100; i++) {

            // 🌍 국가 랜덤 선택
            CountrySpec country = COUNTRIES.get(random.nextInt(COUNTRIES.size()));

            // 📞 번호 생성
            StringBuilder number = new StringBuilder(country.prefix);
            for (int j = 0; j < country.length; j++) {
                number.append(random.nextInt(10));
            }

            String input = number.toString();

            TestCase tc = new TestCase();
            tc.input = input;
            tc.region = country.region;

            try {
                Phonenumber.PhoneNumber num = phoneUtil.parse(input, country.region);

                tc.expectedValid = phoneUtil.isValidNumber(num);

                if (tc.expectedValid) {
                    tc.expectedType = phoneUtil.getNumberType(num).name();
                    tc.expectedE164 = phoneUtil.format(
                            num, PhoneNumberUtil.PhoneNumberFormat.E164);
                }

            } catch (Exception e) {
                tc.expectedValid = false;
            }

            dataset.add(tc);
        }

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File("src/test/resources/golden-dataset.json"), dataset);

        System.out.println("dataset 생성 완료 (multi-country)");
    }
}