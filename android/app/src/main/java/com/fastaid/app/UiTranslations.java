package com.fastaid.app;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class UiTranslations {
    private static final String[] GLOBAL_LANGUAGES = {"English"};
    private static final String[] INDIA_LANGUAGES = {
            "English", "Hindi", "Bengali", "Telugu", "Marathi", "Tamil",
            "Gujarati", "Urdu", "Kannada", "Odia", "Malayalam"
    };
    private static final String[] ENGLISH = {
            "Map", "Nearby", "Incidents", "Profile", "Nearby Aid", "See all",
            "Accident", "Breakdown", "Fuel", "Medical", "Police", "Fire", "Repair",
            "Tyres", "Clinic", "Pharmacy", "Toilets", "NGO", "Safety profile",
            "Use current location", "Share my location", "Call emergency",
            "Voice command", "Enter location"
    };
    private static final Map<String, String[]> TRANSLATIONS = new HashMap<>();

    static {
        add("Hindi", "मानचित्र", "नज़दीक", "घटनाएं", "प्रोफ़ाइल", "नज़दीकी सहायता", "सभी देखें",
                "दुर्घटना", "खराबी", "ईंधन", "चिकित्सा", "पुलिस", "दमकल", "मरम्मत",
                "टायर", "क्लिनिक", "फार्मेसी", "शौचालय", "एनजीओ", "सुरक्षा प्रोफ़ाइल",
                "वर्तमान स्थान उपयोग करें", "मेरा स्थान साझा करें", "आपातकालीन कॉल",
                "वॉइस कमांड", "स्थान दर्ज करें");
        add("Bengali", "মানচিত্র", "কাছাকাছি", "ঘটনা", "প্রোফাইল", "কাছাকাছি সাহায্য", "সব দেখুন",
                "দুর্ঘটনা", "বিকল", "জ্বালানি", "চিকিৎসা", "পুলিশ", "দমকল", "মেরামত",
                "টায়ার", "ক্লিনিক", "ফার্মেসি", "শৌচাগার", "এনজিও", "নিরাপত্তা প্রোফাইল",
                "বর্তমান অবস্থান ব্যবহার করুন", "আমার অবস্থান শেয়ার করুন", "জরুরি কল",
                "ভয়েস কমান্ড", "অবস্থান লিখুন");
        add("Telugu", "మ్యాప్", "దగ్గరలో", "సంఘటనలు", "ప్రొఫైల్", "దగ్గర సహాయం", "అన్నీ చూడండి",
                "ప్రమాదం", "వాహన లోపం", "ఇంధనం", "వైద్యం", "పోలీస్", "అగ్నిమాపక", "మరమ్మత్తు",
                "టైర్లు", "క్లినిక్", "ఫార్మసీ", "మరుగుదొడ్లు", "ఎన్‌జీఓ", "భద్రతా ప్రొఫైల్",
                "ప్రస్తుత స్థానం ఉపయోగించండి", "నా స్థానాన్ని పంచుకోండి", "అత్యవసర కాల్",
                "వాయిస్ కమాండ్", "స్థానం నమోదు చేయండి");
        add("Marathi", "नकाशा", "जवळपास", "घटना", "प्रोफाइल", "जवळची मदत", "सर्व पहा",
                "अपघात", "बिघाड", "इंधन", "वैद्यकीय", "पोलीस", "अग्निशमन", "दुरुस्ती",
                "टायर", "क्लिनिक", "फार्मसी", "शौचालय", "एनजीओ", "सुरक्षा प्रोफाइल",
                "सध्याचे स्थान वापरा", "माझे स्थान शेअर करा", "आपत्कालीन कॉल",
                "व्हॉइस कमांड", "स्थान प्रविष्ट करा");
        add("Tamil", "வரைபடம்", "அருகில்", "சம்பவங்கள்", "சுயவிவரம்", "அருகிலுள்ள உதவி", "அனைத்தையும் காண்க",
                "விபத்து", "வாகனக் கோளாறு", "எரிபொருள்", "மருத்துவம்", "காவல்", "தீயணைப்பு", "பழுது",
                "டயர்கள்", "கிளினிக்", "மருந்தகம்", "கழிப்பறைகள்", "என்ஜிஓ", "பாதுகாப்பு சுயவிவரம்",
                "தற்போதைய இருப்பிடத்தைப் பயன்படுத்து", "என் இருப்பிடத்தைப் பகிர்", "அவசர அழைப்பு",
                "குரல் கட்டளை", "இருப்பிடத்தை உள்ளிடு");
        add("Gujarati", "નકશો", "નજીકમાં", "ઘટનાઓ", "પ્રોફાઇલ", "નજીકની મદદ", "બધું જુઓ",
                "અકસ્મત", "બ્રેકડાઉન", "ઇંધણ", "તબીબી", "પોલીસ", "અગ્નિશામક", "રિપેર",
                "ટાયર", "ક્લિનિક", "ફાર્મસી", "શૌચાલય", "એનજીઓ", "સુરક્ષા પ્રોફાઇલ",
                "વર્તમાન સ્થાન વાપરો", "મારું સ્થાન શેર કરો", "કટોકટી કૉલ",
                "વૉઇસ કમાન્ડ", "સ્થાન દાખલ કરો");
        add("Urdu", "نقشہ", "قریب", "واقعات", "پروفائل", "قریبی مدد", "سب دیکھیں",
                "حادثہ", "خرابی", "ایندھن", "طبی", "پولیس", "فائر سروس", "مرمت",
                "ٹائر", "کلینک", "فارمیسی", "بیت الخلا", "این جی او", "حفاظتی پروفائل",
                "موجودہ مقام استعمال کریں", "میرا مقام شیئر کریں", "ہنگامی کال",
                "صوتی کمانڈ", "مقام درج کریں");
        add("Kannada", "ನಕ್ಷೆ", "ಹತ್ತಿರ", "ಘಟನೆಗಳು", "ಪ್ರೊಫೈಲ್", "ಹತ್ತಿರದ ನೆರವು", "ಎಲ್ಲವನ್ನೂ ನೋಡಿ",
                "ಅಪಘಾತ", "ವಾಹನ ಕೆಟ್ಟಿದೆ", "ಇಂಧನ", "ವೈದ್ಯಕೀಯ", "ಪೊಲೀಸ್", "ಅಗ್ನಿಶಾಮಕ", "ದುರಸ್ತಿ",
                "ಟೈರ್", "ಕ್ಲಿನಿಕ್", "ಔಷಧಾಲಯ", "ಶೌಚಾಲಯಗಳು", "ಎನ್‌ಜಿಒ", "ಸುರಕ್ಷತಾ ಪ್ರೊಫೈಲ್",
                "ಪ್ರಸ್ತುತ ಸ್ಥಳ ಬಳಸಿ", "ನನ್ನ ಸ್ಥಳ ಹಂಚಿಕೊಳ್ಳಿ", "ತುರ್ತು ಕರೆ",
                "ಧ್ವನಿ ಆದೇಶ", "ಸ್ಥಳ ನಮೂದಿಸಿ");
        add("Odia", "ମାନଚିତ୍ର", "ନିକଟରେ", "ଘଟଣା", "ପ୍ରୋଫାଇଲ", "ନିକଟ ସହାୟତା", "ସବୁ ଦେଖନ୍ତୁ",
                "ଦୁର୍ଘଟଣା", "ଗାଡ଼ି ଖରାପ", "ଇନ୍ଧନ", "ଚିକିତ୍ସା", "ପୋଲିସ", "ଅଗ୍ନିଶମ", "ମରାମତି",
                "ଟାୟାର", "କ୍ଲିନିକ", "ଫାର୍ମାସି", "ଶୌଚାଳୟ", "ଏନଜିଓ", "ସୁରକ୍ଷା ପ୍ରୋଫାଇଲ",
                "ବର୍ତ୍ତମାନ ସ୍ଥାନ ବ୍ୟବହାର କରନ୍ତୁ", "ମୋ ସ୍ଥାନ ସେୟାର କରନ୍ତୁ", "ଜରୁରୀ କଲ୍",
                "ଭଏସ୍ କମାଣ୍ଡ", "ସ୍ଥାନ ଲେଖନ୍ତୁ");
        add("Malayalam", "മാപ്പ്", "സമീപം", "സംഭവങ്ങൾ", "പ്രൊഫൈൽ", "സമീപ സഹായം", "എല്ലാം കാണുക",
                "അപകടം", "വാഹന തകരാർ", "ഇന്ധനം", "ചികിത്സ", "പോലീസ്", "അഗ്നിരക്ഷ", "അറ്റകുറ്റപ്പണി",
                "ടയർ", "ക്ലിനിക്", "ഫാർമസി", "ശൗചാലയങ്ങൾ", "എൻജിഒ", "സുരക്ഷാ പ്രൊഫൈൽ",
                "നിലവിലെ സ്ഥാനം ഉപയോഗിക്കുക", "എന്റെ സ്ഥാനം പങ്കിടുക", "അടിയന്തര കോൾ",
                "വോയ്സ് കമാൻഡ്", "സ്ഥാനം നൽകുക");
    }

    private UiTranslations() {
    }

    private static void add(String language, String... values) {
        TRANSLATIONS.put(language, values);
    }

    static String translate(String language, String english) {
        if (english == null || language == null || "English".equals(language)) return english;
        String[] values = TRANSLATIONS.get(language);
        if (values == null) return english;
        for (int index = 0; index < ENGLISH.length && index < values.length; index++) {
            if (ENGLISH[index].equals(english)) return values[index];
        }
        return english;
    }

    static String[] languagesForCountry(String countryCode) {
        if (countryCode != null && "IN".equals(countryCode.trim().toUpperCase(Locale.ROOT))) {
            return INDIA_LANGUAGES.clone();
        }
        return GLOBAL_LANGUAGES.clone();
    }

    static boolean isLanguageAllowedForCountry(String language, String countryCode) {
        if (language == null) return false;
        for (String allowed : languagesForCountry(countryCode)) {
            if (allowed.equals(language)) return true;
        }
        return false;
    }

    static String localeTag(String language) {
        if ("Hindi".equals(language)) return "hi-IN";
        if ("Bengali".equals(language)) return "bn-IN";
        if ("Telugu".equals(language)) return "te-IN";
        if ("Marathi".equals(language)) return "mr-IN";
        if ("Tamil".equals(language)) return "ta-IN";
        if ("Gujarati".equals(language)) return "gu-IN";
        if ("Urdu".equals(language)) return "ur-IN";
        if ("Kannada".equals(language)) return "kn-IN";
        if ("Odia".equals(language)) return "or-IN";
        if ("Malayalam".equals(language)) return "ml-IN";
        return Locale.getDefault().toLanguageTag();
    }

    static boolean isRtl(String language) {
        return "Urdu".equals(language);
    }
}
