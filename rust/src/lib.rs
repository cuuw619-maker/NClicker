use jni::objects::JClass;
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;

#[derive(Clone, Copy)]
struct Country {
    name: &'static str,
    code: &'static str,
    flag: &'static str,
    regions: &'static [&'static str],
}

const COUNTRIES: [Country; 10] = [
    Country { name: "Россия", code: "RU", flag: "🇷🇺", regions: &["Москва · 77", "Санкт-Петербург · 78", "Татарстан · 16", "Краснодар · 23", "Свердловская область · 66"] },
    Country { name: "Украина", code: "UA", flag: "🇺🇦", regions: &["Киев · AA", "Львов · BC", "Одесса · BH", "Харьков · AX", "Днепр · AE"] },
    Country { name: "Германия", code: "DE", flag: "🇩🇪", regions: &["Берлин · B", "Гамбург · HH", "Мюнхен · M", "Кёльн · K"] },
    Country { name: "Япония", code: "JP", flag: "🇯🇵", regions: &["Токио · 品川", "Осака · なにわ", "Йокогама · 横浜"] },
    Country { name: "США", code: "US", flag: "🇺🇸", regions: &["Калифорния · CA", "Нью-Йорк · NY", "Техас · TX", "Флорида · FL"] },
    Country { name: "Великобритания", code: "GB", flag: "🇬🇧", regions: &["Лондон · LD", "Манчестер · MN", "Уэльс · CY"] },
    Country { name: "Франция", code: "FR", flag: "🇫🇷", regions: &["Париж · 75", "Лион · 69", "Марсель · 13"] },
    Country { name: "ОАЭ", code: "AE", flag: "🇦🇪", regions: &["Дубай · D", "Абу-Даби · AU", "Шарджа · SH"] },
    Country { name: "Польша", code: "PL", flag: "🇵🇱", regions: &["Варшава · WA", "Краков · KR", "Гданьск · GD"] },
    Country { name: "Италия", code: "IT", flag: "🇮🇹", regions: &["Рим · RM", "Милан · MI", "Турин · TO"] },
];

const RARITY_NAMES: [&str; 7] = [
    "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "SECRET",
];
const BASE_MARKET_VALUES: [u64; 7] = [100, 150, 300, 750, 2_000, 7_500, 25_000];

/// Seeded lightweight PRNG. All data stays local; the seed combines a
/// monotonic clock value and a Java-side sequence.
struct GameRng(u64);

impl GameRng {
    fn new(seed: u64) -> Self {
        let mixed = mix64(seed ^ 0x4E43_4C49_434B_4552);
        Self(if mixed == 0 { 0xA076_1D64_78BD_642F } else { mixed })
    }

    fn next_u64(&mut self) -> u64 {
        self.0 ^= self.0 >> 12;
        self.0 ^= self.0 << 25;
        self.0 ^= self.0 >> 27;
        self.0.wrapping_mul(0x2545_F491_4F6C_DD1D)
    }

    fn range(&mut self, upper: u64) -> u64 {
        if upper <= 1 { 0 } else { self.next_u64() % upper }
    }

    fn digits(&mut self, count: usize) -> String {
        (0..count).map(|_| char::from(b'0' + self.range(10) as u8)).collect()
    }

    fn latin_letter(&mut self) -> &'static str {
        const LETTERS: [&str; 23] = [
            "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M",
            "N", "P", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
        ];
        LETTERS[self.range(LETTERS.len() as u64) as usize]
    }

    fn latin_letters(&mut self, count: usize) -> String {
        (0..count).map(|_| self.latin_letter()).collect()
    }

    fn russian_letter(&mut self) -> &'static str {
        const LETTERS: [&str; 12] = ["А", "В", "Е", "К", "М", "Н", "О", "Р", "С", "Т", "У", "Х"];
        LETTERS[self.range(LETTERS.len() as u64) as usize]
    }

    fn russian_letters(&mut self, count: usize) -> String {
        (0..count).map(|_| self.russian_letter()).collect()
    }
}

fn mix64(mut value: u64) -> u64 {
    value = value.wrapping_add(0x9E37_79B9_7F4A_7C15);
    value = (value ^ (value >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
    value = (value ^ (value >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
    value ^ (value >> 31)
}

fn rarity_weights(luck: u32) -> [u64; 7] {
    let level = luck.min(12) as f64;
    [
        ((60_000.0 / (1.0 + level * 0.10)) as u64).max(4_000),
        24_000,
        (12_000.0 * (1.0 + level * 0.06)) as u64,
        (3_000.0 * (1.0 + level * 0.10)) as u64,
        (800.0 * (1.0 + level * 0.13)) as u64,
        (170.0 * (1.0 + level * 0.16)) as u64,
        (30.0 * (1.0 + level * 0.20)) as u64,
    ]
}

fn roll_rarity(rng: &mut GameRng, luck: u32) -> usize {
    let weights = rarity_weights(luck);
    let mut ticket = rng.range(weights.iter().sum());
    for (index, weight) in weights.iter().enumerate() {
        if ticket < *weight {
            return index;
        }
        ticket -= *weight;
    }
    weights.len() - 1
}

fn roll_type(rng: &mut GameRng, rarity: usize) -> &'static str {
    let value = rng.range(10_000);
    if rarity >= 4 && value < 1_200 {
        "Чёрная коллекционная серия"
    } else if value < 180 {
        "ФСБ · игровая спецсерия"
    } else if value < 700 {
        "Полиция · игровой выпуск"
    } else if value < 1_300 {
        "Чёрная серия"
    } else if value < 2_400 {
        "Такси"
    } else if value < 3_200 {
        "Дипломатический"
    } else if value < 4_200 {
        "Ретро"
    } else {
        "Гражданский"
    }
}

/// Generate familiar patterns; the values are random and not issued registrations.
fn make_number(rng: &mut GameRng, code: &str, region: &str) -> String {
    let suffix = region.rsplit('·').next().unwrap_or(region).trim();
    match code {
        // Russia: one Cyrillic letter, three digits, two Cyrillic letters, region.
        "RU" => format!("{}{}{} {}", rng.russian_letters(1), rng.digits(3), rng.russian_letters(2), suffix),
        // Ukraine: regional prefix, four digits, two Latin letters.
        "UA" => format!("{} {} {}", suffix, rng.digits(4), rng.latin_letters(2)),
        // Germany: registration district, one/two letters, four digits.
        "DE" => format!("{} {} {}", suffix, rng.latin_letters(2), rng.digits(4)),
        // Japan: class number, kana, and a two-pair serial.
        "JP" => format!("503 ね {}-{}", rng.digits(2), rng.digits(2)),
        // Typical California-style plate or state-specific 3 letters + 4 digits.
        "US" if suffix == "NY" || suffix == "TX" || suffix == "FL" => {
            format!("{}-{}", rng.latin_letters(3), rng.digits(4))
        }
        "US" => format!("{}{}{}", rng.digits(1), rng.latin_letters(3), rng.digits(3)),
        // United Kingdom: two letters, age identifier, three letters.
        "GB" => format!("{}{} {}", rng.latin_letters(2), rng.digits(2), rng.latin_letters(3)),
        // France: AA-123-AA; Poland: region + 3 digits + 2 letters.
        "FR" => format!("{}-{}-{}", rng.latin_letters(2), rng.digits(3), rng.latin_letters(2)),
        "AE" => rng.digits(5),
        "PL" => format!("{} {}{}", suffix, rng.digits(3), rng.latin_letters(2)),
        "IT" => format!("{} {} {}", rng.latin_letters(2), rng.digits(3), rng.latin_letters(2)),
        _ => format!("NC {} ZZ", rng.digits(3)),
    }
}

fn category_value_multiplier(kind: &str) -> f64 {
    if kind.contains("Чёрная") {
        1.35
    } else if kind.starts_with("ФСБ") || kind.starts_with("Полиция") {
        1.25
    } else if kind == "Такси" {
        0.90
    } else if kind == "Дипломатический" {
        1.60
    } else if kind == "Ретро" {
        1.40
    } else {
        1.0
    }
}

fn generate_plate(seed: u64, luck: u32) -> String {
    let mut rng = GameRng::new(seed);
    let rarity_index = roll_rarity(&mut rng, luck);
    let kind = roll_type(&mut rng, rarity_index);
    let country_index = if kind.starts_with("ФСБ") {
        0
    } else {
        rng.range(COUNTRIES.len() as u64) as usize
    };
    let country = COUNTRIES[country_index];
    let region = country.regions[rng.range(country.regions.len() as u64) as usize];
    let number = make_number(&mut rng, country.code, region);
    let market_value = (BASE_MARKET_VALUES[rarity_index] as f64
        * category_value_multiplier(kind))
        .round()
        .max(50.0) as u64;
    let id = format!("NC-{}-{:08X}", country.code, (rng.next_u64() & 0xFFFF_FFFF) as u32);

    // Interpolated values come only from static catalogues and controlled
    // digit/letter alphabets, so they cannot inject JSON delimiters.
    format!(
        "{{\"id\":\"{}\",\"country\":\"{}\",\"countryCode\":\"{}\",\"flag\":\"{}\",\"region\":\"{}\",\"number\":\"{}\",\"type\":\"{}\",\"rarity\":\"{}\",\"marketValue\":{}}}",
        id, country.name, country.code, country.flag, region, number, kind, RARITY_NAMES[rarity_index], market_value
    )
}

#[no_mangle]
pub extern "system" fn Java_com_nclicker_game_PlateEngine_nativeGenerate<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    seed: jlong,
    luck_level: jint,
    _broker_level: jint,
) -> jstring {
    let output = generate_plate(seed as u64, luck_level.max(0).min(12) as u32);
    match env.new_string(output) {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn same_seed_generates_same_plate() {
        assert_eq!(generate_plate(123_456, 4), generate_plate(123_456, 4));
    }

    #[test]
    fn output_contains_market_value_and_required_fields() {
        let plate = generate_plate(0xABCD_1234, 0);
        for field in ["\"country\"", "\"region\"", "\"number\"", "\"rarity\"", "\"marketValue\""] {
            assert!(plate.contains(field), "missing field {field}: {plate}");
        }
    }

    #[test]
    fn luck_changes_rarity_distribution_weights() {
        let base = rarity_weights(0);
        let upgraded = rarity_weights(8);
        assert!(upgraded[0] < base[0]);
        assert!(upgraded[2] > base[2]);
        assert!(upgraded[6] > base[6]);
    }

    #[test]
    fn each_rarity_has_a_market_price() {
        assert_eq!(BASE_MARKET_VALUES[0], 100);
        assert!(BASE_MARKET_VALUES.windows(2).all(|pair| pair[1] > pair[0]));
    }

    #[test]
    fn generated_plate_uses_a_known_country_code() {
        let plate = generate_plate(91_234, 2);
        assert!(plate.contains("\"countryCode\":\""));
        assert!(plate.contains("\"marketValue\":"));
    }
}
