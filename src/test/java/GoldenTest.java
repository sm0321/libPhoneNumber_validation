import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GoldenTest {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    static class TestCase {
        public String input;
        public String region;
        public String caseType;

        public boolean expectedValid;
        public String expectedType;
        public String expectedE164;

        public Integer parsedCountryCode;
        public Long parsedNationalNumber;
        public String parsedRaw;
    }

    @Test
    void runTest() throws Exception {

        List<TestCase> dataset =
                mapper.readValue(
                        getClass().getClassLoader()
                                .getResourceAsStream("golden-dataset.json"),
                        new TypeReference<List<TestCase>>() {}
                );

        for (TestCase tc : dataset) {

            try {
                Phonenumber.PhoneNumber num =
                        phoneUtil.parse(tc.input, tc.region);

                boolean valid = phoneUtil.isValidNumber(num);

                // =========================
                // ✅ valid 검증
                // =========================
                assertEquals(tc.expectedValid, valid,
                        "VALID mismatch: " + tc.input + " (" + tc.region + ")");

                // =========================
                // ✅ valid한 경우만 상세 검증
                // =========================
                if (valid) {

                    assertEquals(tc.expectedType,
                            phoneUtil.getNumberType(num).name(),
                            "TYPE mismatch: " + tc.input);

                    assertEquals(tc.expectedE164,
                            phoneUtil.format(num,
                                    PhoneNumberUtil.PhoneNumberFormat.E164),
                            "E164 mismatch: " + tc.input);
                }

                // =========================
                // 🔥 핵심: parse 결과 검증
                // =========================
                assertEquals(tc.parsedCountryCode,
                        num.getCountryCode(),
                        "countryCode mismatch: " + tc.input);

                assertEquals(tc.parsedNationalNumber,
                        num.getNationalNumber(),
                        "nationalNumber mismatch: " + tc.input);

                assertEquals(tc.parsedRaw,
                        num.toString(),
                        "parsedRaw mismatch: " + tc.input);

            } catch (Exception e) {

                // =========================
                // 🔥 parse 실패 케이스
                // =========================
                assertFalse(tc.expectedValid,
                        "Expected valid but parse failed: " + tc.input);

                assertEquals("PARSE_ERROR", tc.parsedRaw,
                        "Expected PARSE_ERROR: " + tc.input);
            }
        }
    }
}