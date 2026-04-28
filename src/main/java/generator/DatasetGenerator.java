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

    public static void main(String[] args) throws Exception {

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        ObjectMapper mapper = new ObjectMapper();

        List<TestCase> dataset = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 100; i++) {
            String input = "010" + (10000000 + random.nextInt(90000000));

            TestCase tc = new TestCase();
            tc.input = input;
            tc.region = "KR";

            try {
                Phonenumber.PhoneNumber num = phoneUtil.parse(input, "KR");

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
                .writeValue(new File("dataset/golden-dataset.json"), dataset);

        System.out.println("dataset 생성 완료");
    }
}