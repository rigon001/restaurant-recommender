import spacy
from spacy.pipeline import EntityRuler
from collections import defaultdict, Counter
import json


def parse_user_prefs_from_xml(xml_content):
    """
    Parse user preferences from XML content.

    :param xml_content: str - XML content as a string.
    :return: dict - Parsed user preferences as a dictionary.
    """
    root = ET.fromstring(xml_content)
    user_prefs = {}
    for child in root.findall("string"):
        key = child.attrib.get("name")
        value = child.text
        # Convert JSON-like strings back to Python lists
        try:
            user_prefs[key] = json.loads(value.replace("&quot;", '"'))
        except json.JSONDecodeError:
            user_prefs[key] = value
    return user_prefs


def analyze_user_context(user_prefs):
    """
    Analyze the user's preferences to find the most dominant terms.

    :param user_prefs: dict - Parsed user preferences.
    :return: dict - Dominant preferences by category.
    """
    dominant_preferences = {}
    categories = {
        "CUISINE": ["user_styles", "restaurant_styles"],
        "PRICE": ["user_prices", "restaurant_prices"],
        "MEALS": ["user_meals", "restaurant_meals"],
        "FEATURE": ["user_features", "restaurant_features"],
        "RATING": ["user_ratings", "restaurant_ratings"]
    }

    for category, keys in categories.items():
        combined_list = []
        for key in keys:
            combined_list.extend(user_prefs.get(key, []))
        if combined_list:
            # Find the most common item in the combined list
            most_common = Counter(combined_list).most_common(1)
            if most_common:
                dominant_preferences[category] = most_common[0][0]  # Get the most frequent term

    return dominant_preferences


def load_spacy_model_with_entity_ruler(entity_ruler_path):
    """
    Load the SpaCy model and attach the EntityRuler from disk.
    """
    try:
        nlp = spacy.load("en_core_web_sm")
        # Load EntityRuler patterns
        ruler = EntityRuler(nlp).from_disk(entity_ruler_path)
        nlp.add_pipe(ruler, before="ner")
        print("SpaCy model and EntityRuler loaded successfully.")
        return nlp
    except Exception as e:
        print(f"Error loading SpaCy model or EntityRuler: {e}")
        raise


def extract_entities_from_query(query, nlp):
    """
    Extract categorized entities (e.g., STYLE, PRICE) from the query.

    :param query: str - The user's initial search query.
    :param nlp: spacy.Language - SpaCy NLP model with EntityRuler.
    :return: dict - Extracted entities categorized by their labels.
    """
    doc = nlp(query)
    extracted_entities = defaultdict(list)
    for ent in doc.ents:
        extracted_entities[ent.label_].append(ent.text.lower())
    return extracted_entities


def enhance_query(initial_query, user_prefs_json, entity_ruler_path):
    """
    Enhance the query dynamically based on extracted terms and user preferences.

    :param initial_query: str - The user's initial search query.
    :param user_prefs_json: str - JSON string containing user preferences.
    :param entity_ruler_path: str - Path to the EntityRuler patterns on disk.
    :return: str - The enhanced query.
    """
    # Parse user preferences from JSON
    user_prefs = json.loads(user_prefs_json)

    # Analyze user context
    dominant_preferences = analyze_user_context(user_prefs)
    print(f"Dominant Preferences: {dominant_preferences}")

    # Load SpaCy model with EntityRuler
    nlp = load_spacy_model_with_entity_ruler(entity_ruler_path)

    # Extract entities from the initial query
    extracted_entities = extract_entities_from_query(initial_query, nlp)
    print(f"Extracted Entities: {dict(extracted_entities)}")

    # Complement the query
    enhanced_query = initial_query

    for category, dominant_term in dominant_preferences.items():
        # Add the dominant preference if the category is not already present in the query
        if category not in extracted_entities:
            if category == "STYLE":
                enhanced_query += f" for {dominant_term} cuisine"
            elif category == "PRICE":
                enhanced_query += f" that is {dominant_term} priced"
            elif category == "FEATURE":
                enhanced_query += f" with {dominant_term}"
            elif category == "MEALS":
                enhanced_query += f" for {dominant_term}"

    return enhanced_query