package dev.blitical.jigsawDB.tests;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.ExceptionHandler;
import dev.blitical.jigsawDB.Tests;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.tables.NoCachingTable;
import dev.blitical.jigsawDB.tables.NoCachingTable.TestEnum;
import dev.blitical.jigsawDB.tables.NoCachingTableFields;
import dev.blitical.jigsawDB.util.JSONClass;
import dev.blitical.jigsawDB.util.SerializableClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(ExceptionHandler.class)
public class SetAndGetTests {
    private static final Map<String, String> STRINGS_TO_TEST = Map.of(
            "regular", "Hello there! This is just a regular string!",
            "punctuation", "Hello there I'm a string with a thingy punctuation mark; this one: '",
            "escaping characters", "Hi! I have an escaping character cuz sally said \"Hello there!\"",
            "new lines", "Hi!\nI'm testing new lines.\n cuz new lines are\n\nCOOL!",
            "trailing new line", "What about a trailing new line? \n",
            "windows new line", "The other new line \r",
            "windows + regular new line", "Other other new line \r\n",
            "super long string", "Lets test a SUPPPEEERRR LONNNNGGGG SSSTTTRRRIIINNNGGGGG: "
                    + Stream.generate(() -> UUID.randomUUID().toString())
                    .limit(500L)
                    .collect(Collectors.joining(UUID.randomUUID().toString())),
            "special characters", """
                    | 😀😃😄😁😆😅😂🤣😊😉😍🤖👾👻💀☠️
                    | 你好世界 こんにちは 안녕하세요
                    | Привет мир
                    | Γειά σου Κόσμε
                    | ∑ ∏ √ ∞ ≈ ≠ ≤ ≥ ± → ← ⇌
                    | © ® ™ ✓ ✗ ★ ☆ • § ¶ † ‡
                    | $ € £ ¥ ₹ ₿ | \\ " \\' \t  \r
                    | <tag attr="value">&</tag>
                    | \u200b\u200c\u200d
                    | \\u200B\\u200C\\u200D
                    | אבגדה
                    | á é ö ñ
                    | \u0000 \u001b"""
    );

    private static final Map<String, Integer> INTEGERS_TO_TEST = Map.of(
            "regular", 12478912,
            "regular negative", -480182034,
            "zero", 0,
            "min", Integer.MIN_VALUE,
            "max", Integer.MAX_VALUE
    );

    private static final Map<String, Long> LONGS_TO_TEST = Map.of(
            "regular", 124789124324891294L,
            "regular negative", -4801820342412840192L,
            "zero", 0L,
            "min", Long.MIN_VALUE,
            "max", Long.MAX_VALUE
    );

    private static final Map<String, NoCachingTable.TestEnum> ENUMS_TO_TEST = Map.of(
            "one (min)", TestEnum.ONE,
            "two", TestEnum.TWO,
            "three", TestEnum.THREE,
            "four", TestEnum.FOUR,
            "five (max)", TestEnum.FIVE
    );

    @BeforeAll
    static void before() {
        Tests.setUpDatabases(new NoCachingTable());
    }

    @AfterAll
    static void after() {
        Tests.destroy();
    }

    @Test
    void stringSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();

            for (var e : STRINGS_TO_TEST.entrySet()) {
                String id = e.getKey();
                String string = e.getValue();
                entry.set(NoCachingTableFields.string, string).queue();
                String compare = entry.get(NoCachingTableFields.string).complete();
                if (!string.equals(compare)) {
                    throw new IllegalStateException(String.format("Mismatch database values in testing 'string': \"%s\" != \"%s\"", string, compare));
                }

                JigsawDBLogger.info("Passed StringSetAndGet test '%s'", id);
            }
        }

    }

    @Test
    void integerSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();

            for (var e : INTEGERS_TO_TEST.entrySet()) {
                String id = e.getKey();
                Integer num = e.getValue();
                entry.set(NoCachingTableFields.integer, num).queue();
                Integer compare = entry.get(NoCachingTableFields.integer).complete();
                if (!num.equals(compare)) {
                    throw new IllegalStateException(String.format("Mismatch database values in testing 'integer': \"%s\" != \"%s\"", num, compare));
                }

                JigsawDBLogger.info("Passed IntegerSetAndGet test '%s'", id);
            }
        }

    }

    @Test
    void longSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();

            for (var e : LONGS_TO_TEST.entrySet()) {
                String id = e.getKey();
                Long num = e.getValue();
                entry.set(NoCachingTableFields.longValue, num).queue();
                Long compare = entry.get(NoCachingTableFields.longValue).complete();
                if (!num.equals(compare)) {
                    throw new IllegalStateException(String.format("Mismatch database values in testing 'long': \"%s\" != \"%s\"", num, compare));
                }

                JigsawDBLogger.info("Passed LongSetAndGet test '%s'", id);
            }
        }

    }

    @Test
    void enumOrdinalSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();

            for (var e : ENUMS_TO_TEST.entrySet()) {
                String id = e.getKey();
                TestEnum enumVal = e.getValue();
                entry.set(NoCachingTableFields.testEnumOrdinal, enumVal).queue();
                TestEnum compare = entry.get(NoCachingTableFields.testEnumOrdinal).complete();
                if (!enumVal.equals(compare)) {
                    throw new IllegalStateException(String.format("Mismatch database values in testing 'enum-ordinal': \"%s\" != \"%s\"", enumVal, compare));
                }

                JigsawDBLogger.info("Passed EnumOrdinalSetAndGet test '%s'", id);
            }
        }

    }

    @Test
    void enumStringSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();

            for (var e : ENUMS_TO_TEST.entrySet()) {
                String id = e.getKey();
                TestEnum enumVal = e.getValue();
                entry.set(NoCachingTableFields.testEnumString, enumVal).queue();
                TestEnum compare = entry.get(NoCachingTableFields.testEnumString).complete();
                if (!enumVal.equals(compare)) {
                    throw new IllegalStateException(String.format("Mismatch database values in testing 'enum-string': \"%s\" != \"%s\"", enumVal, compare));
                }

                JigsawDBLogger.info("Passed EnumStringSetAndGet test '%s'", id);
            }
        }

    }

    @Test
    void temporalEpochSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();
            OffsetDateTime min = OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.MIN_VALUE), ZoneOffset.UTC);
            entry.set(NoCachingTableFields.timeEpoch, min).queue();
            OffsetDateTime compare = entry.get(NoCachingTableFields.timeEpoch).complete();
            if (!min.equals(compare)) {
                throw new IllegalStateException(String.format("Mismatch database values in testing 'temporal-epoch': \"%s\" != \"%s\"", min, compare));
            }

            OffsetDateTime max = OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), ZoneOffset.UTC);
            entry.set(NoCachingTableFields.timeEpoch, max).queue();
            OffsetDateTime compare2 = entry.get(NoCachingTableFields.timeEpoch).complete();
            if (!max.equals(compare2)) {
                throw new IllegalStateException(String.format("Mismatch database values in testing 'temporal-epoch': \"%s\" != \"%s\"", max, compare2));
            }

            JigsawDBLogger.info("Passed TemporalEpochSetAndGet test", new Object[0]);
        }

    }

    @Test
    void temporalISOSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();
            OffsetDateTime min = OffsetDateTime.MIN;
            entry.set(NoCachingTableFields.timeISO, min).queue();
            OffsetDateTime compare = entry.get(NoCachingTableFields.timeISO).complete();
            if (!min.equals(compare)) {
                throw new IllegalStateException(String.format("Mismatch database values in testing 'temporal-epoch': \"%s\" != \"%s\"", min, compare));
            }

            OffsetDateTime max = OffsetDateTime.MAX;
            entry.set(NoCachingTableFields.timeISO, max).queue();
            OffsetDateTime compare2 = entry.get(NoCachingTableFields.timeISO).complete();
            if (!max.equals(compare2)) {
                throw new IllegalStateException(String.format("Mismatch database values in testing 'temporal-ISO': \"%s\" != \"%s\"", max, compare2));
            }

            JigsawDBLogger.info("Passed TemporalISOSetAndGet test");
        }

    }

    @Test
    void javaSerializedSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();
            SerializableClass clazz = new SerializableClass();
            clazz.first = STRINGS_TO_TEST.get("super long string");
            clazz.second = STRINGS_TO_TEST.get("special characters");
            clazz.third = Integer.MIN_VALUE;
            clazz.fourth = Long.MAX_VALUE;
            clazz.fifth = true;
            clazz.chained = new SerializableClass.Chained();
            clazz.chained.first = STRINGS_TO_TEST.get("windows + regular new line");
            clazz.chained.second = Integer.MIN_VALUE;
            clazz.chained.third = false;
            entry.set(NoCachingTableFields.testSerializableClass, clazz).queue();
            SerializableClass compare = entry.get(NoCachingTableFields.testSerializableClass).complete();
            if (compare == null) {
                throw new IllegalStateException(String.format("Mismatch database values in testing 'SerializableClass-OVERALL", clazz, null));
            }

            for (Field field : SerializableClass.class.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object a = field.get(clazz);
                    Object b = field.get(compare);
                    if (field.getType().equals(SerializableClass.Chained.class)) {
                        for (Field field2 : SerializableClass.Chained.class.getDeclaredFields()) {
                            field2.setAccessible(true);
                            Object a2 = ((SerializableClass.Chained) a).first;
                            Object b2 = ((SerializableClass.Chained) b).first;
                            if (!Objects.equals(a2, b2)) {
                                throw new IllegalStateException(String.format("Mismatch database values in testing 'SerializableClass-CHAINED-%s: \"%s\" != \"%s\"", field.getName(), a2, b2));
                            }
                        }
                    } else if (!Objects.equals(a, b)) {
                        throw new IllegalStateException(String.format("Mismatch database values in testing 'SerializableClass-%s: \"%s\" != \"%s\"", field.getName(), a, b));
                    }
                } catch (IllegalAccessException var18) {
                    throw new RuntimeException("Triggered IllegalAccessException (should be impossible)");
                }
            }

            JigsawDBLogger.info("Passed JavaSerializedSetAndGet test");
        }

    }

    @Test
    void JSONClassSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();
            JSONClass clazz = new JSONClass();
            clazz.first = STRINGS_TO_TEST.get("super long string");
            clazz.second = STRINGS_TO_TEST.get("special characters");
            clazz.third = Integer.MIN_VALUE;
            clazz.fourth = Long.MAX_VALUE;
            clazz.fifth = true;
            clazz.chained = new JSONClass.Chained();
            clazz.chained.first = STRINGS_TO_TEST.get("windows + regular new line");
            clazz.chained.second = Integer.MIN_VALUE;
            clazz.chained.third = false;
            entry.set(NoCachingTableFields.testJSONClass, clazz).queue();
            JSONClass compare = entry.get(NoCachingTableFields.testJSONClass).complete();
            if (compare == null) {
                throw new IllegalStateException(String.format("Mismatch database values in testing 'JSONClass-OVERALL: \"%s\" != \"%s\"", clazz, null));
            }

            for (Field field : JSONClass.class.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object a = field.get(clazz);
                    Object b = field.get(compare);
                    if (field.getType().equals(JSONClass.Chained.class)) {
                        for (Field field2 : JSONClass.Chained.class.getDeclaredFields()) {
                            field2.setAccessible(true);
                            Object a2 = ((JSONClass.Chained) a).first;
                            Object b2 = ((JSONClass.Chained) b).first;
                            if (!Objects.equals(a2, b2)) {
                                throw new IllegalStateException(String.format("Mismatch database values in testing 'JSONClass-CHAINED-%s: \"%s\" != \"%s\"", field.getName(), a2, b2));
                            }
                        }
                    } else if (!Objects.equals(a, b)) {
                        throw new IllegalStateException(String.format("Mismatch database values in testing 'JSONClass-%s: \"%s\" != \"%s\"", field.getName(), a, b));
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Triggered IllegalAccessException (should be impossible)");
                }
            }

            JigsawDBLogger.info("Passed JSONClassSetAndGet test");
        }

    }

    @Test
    void BinaryInputStreamSetAndGet() {
        for (ConnectedDatabase d : Tests.databases) {
            var entry = d.getOrCreateEntry(NoCachingTable.class, Tests.TESTING_ENTRY_UUID).complete();
            String hash1;
            String hash2;

            try (InputStream raw = this.getClass().getClassLoader().getResourceAsStream("testImage.png")) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");

                try (InputStream stream = new DigestInputStream(raw, digest)) {
                    entry.setWithInputStream(NoCachingTableFields.image, stream).complete();
                }

                hash1 = bytesToHex(digest.digest());
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }

            try (InputStream stream = entry.getAsInputStream(NoCachingTableFields.image).complete()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[8192];

                int read;
                while ((read = stream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }

                hash2 = bytesToHex(digest.digest());
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }

            if (!hash1.equals(hash2)) {
                throw new IllegalStateException(String.format(
                        "Mismatch database values in testing 'binary-inputStream': \"%s\" != \"%s\"",
                        hash1,
                        hash2
                ));
            }
        }

    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
}
