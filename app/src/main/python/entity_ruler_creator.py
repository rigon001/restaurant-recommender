import spacy
from spacy.pipeline import EntityRuler

def create_entity_ruler():
    """
    Create and save the EntityRuler in the device's internal storage.
    """
    try:
        # Load SpaCy model
        nlp = spacy.load("en_core_web_sm")

        # Create the EntityRuler
        ruler = EntityRuler(nlp)

        # Define patterns
        # Define static patterns
        patterns = [
            # Cuisines
            {"label": "CUISINE", "pattern": [{"LOWER": "italian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "chinese"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "mexican"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "indian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "japanese"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "thai"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "french"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "greek"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "spanish"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "korean"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "vietnamese"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "mediterranean"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "american"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "lebanese"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "turkish"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "brazilian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "moroccan"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "caribbean"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "german"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "persian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "ethiopian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "pakistani"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "russian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "filipino"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "cuban"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "indonesian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "malaysian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "afghan"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "polish"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "scandinavian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "portuguese"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "british"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "argentinian"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "chilean"}]},
            {"label": "CUISINE", "pattern": [{"LOWER": "peruvian"}]},

            # Prices
            {"label": "PRICE", "pattern": [{"LOWER": "cheap"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "affordable"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "reasonable"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "inexpensive"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "economical"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "budget"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "low-cost"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "budget-friendly"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "economy"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "family"}, {"LOWER": "budget"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "splurge-worthy"}]},

            # Keywords for mid-range pricing
            {"label": "PRICE", "pattern": [{"LOWER": "mid-range"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "moderate"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "reasonably"}, {"LOWER": "priced"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "fair"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "standard"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "average"}]},

            # Keywords for high-end pricing
            {"label": "PRICE", "pattern": [{"LOWER": "expensive"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "luxury"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "high-end"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "premium"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "exclusive"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "top-tier"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "costly"}]},

            # Cheaper/Lower Price Comparisons
            {"label": "PRICE", "pattern": [{"LOWER": "cheaper"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "cheapest"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "low"}, {"LOWER": "cost"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "less"}, {"LOWER": "expensive"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "least"}, {"LOWER": "expensive"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "more"}, {"LOWER": "affordable"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "most"}, {"LOWER": "affordable"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "better"}, {"LOWER": "value"}]},

            # Expensive/Higher Price Comparisons
            {"label": "PRICE", "pattern": [{"LOWER": "more"}, {"LOWER": "expensive"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "most"}, {"LOWER": "expensive"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "higher"}, {"LOWER": "price"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "costlier"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "pricier"}]},
            {"label": "PRICE", "pattern": [{"LOWER": "top"}, {"LOWER": "price"}]},

            # Common meals
            {"label": "MEALS", "pattern": "breakfast"},
            {"label": "MEALS", "pattern": "brunch"},
            {"label": "MEALS", "pattern": "lunch"},
            {"label": "MEALS", "pattern": "dinner"},
            {"label": "MEALS", "pattern": "supper"},
            {"label": "MEALS", "pattern": "snack"},

            # Meal times
            {"label": "MEALS", "pattern": [{"LOWER": "morning"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "afternoon"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "evening"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "night"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "late"}, {"LOWER": "night"}]},

            # Descriptive meal terms
            {"label": "MEALS", "pattern": [{"LOWER": "quick"}, {"LOWER": "bite"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "light"}, {"LOWER": "meal"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "heavy"}, {"LOWER": "meal"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "small"}, {"LOWER": "meal"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "full"}, {"LOWER": "course"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "set"}, {"LOWER": "menu"}]},

            # Regional/Occasional meals
            {"label": "MEALS", "pattern": "high tea"},
            {"label": "MEALS", "pattern": [{"LOWER": "afternoon"}, {"LOWER": "tea"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "midnight"}, {"LOWER": "snack"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "power"}, {"LOWER": "lunch"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "working"}, {"LOWER": "lunch"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "casual"}, {"LOWER": "dinner"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "buffet"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "banquet"}]},

            # Special meals
            {"label": "MEALS", "pattern": [{"LOWER": "celebration"}, {"LOWER": "dinner"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "romantic"}, {"LOWER": "dinner"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "holiday"}, {"LOWER": "meal"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "anniversary"}, {"LOWER": "dinner"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "birthday"}, {"LOWER": "lunch"}]},

            # Culturally specific terms
            {"label": "MEALS", "pattern": "dim sum"},
            {"label": "MEALS", "pattern": "bento"},
            {"label": "MEALS", "pattern": "tapas"},
            {"label": "MEALS", "pattern": "smorgasbord"},
            {"label": "MEALS", "pattern": "kaiseki"},
            {"label": "MEALS", "pattern": [{"LOWER": "thali"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "mezze"}]},

            # Miscellaneous
            {"label": "MEALS", "pattern": [{"LOWER": "weekend"}, {"LOWER": "brunch"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "family"}, {"LOWER": "meal"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "kids"}, {"LOWER": "meal"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "business"}, {"LOWER": "lunch"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "formal"}, {"LOWER": "dinner"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "informal"}, {"LOWER": "dinner"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "pre"}, {"LOWER": "show"}, {"LOWER": "meal"}]},
            {"label": "MEALS", "pattern": [{"LOWER": "post"}, {"LOWER": "show"}, {"LOWER": "meal"}]},

             # General Features
            {"label": "FEATURE", "pattern": [{"LOWER": "reservations"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "seating"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "serves"}, {"LOWER": "alcohol"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "full"}, {"LOWER": "bar"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "accepts"}, {"LOWER": "credit"}, {"LOWER": "cards"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "table"}, {"LOWER": "service"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "takeout"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "delivery"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "outdoor"}, {"LOWER": "seating"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "wheelchair"}, {"LOWER": "accessible"}]},

            # Payment Methods
            {"label": "FEATURE", "pattern": [{"LOWER": "accepts"}, {"LOWER": "cash"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "accepts"}, {"LOWER": "mobile"}, {"LOWER": "payments"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "accepts"}, {"LOWER": "google"}, {"LOWER": "pay"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "accepts"}, {"LOWER": "apple"}, {"LOWER": "pay"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "contactless"}, {"LOWER": "payments"}]},

            # Ambiance and Style
            {"label": "FEATURE", "pattern": [{"LOWER": "romantic"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "family-friendly"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "casual"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "fine"}, {"LOWER": "dining"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "cozy"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "modern"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "quiet"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "lively"}]},

            # Entertainment and Extras
            {"label": "FEATURE", "pattern": [{"LOWER": "live"}, {"LOWER": "music"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "sports"}, {"LOWER": "screenings"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "happy"}, {"LOWER": "hour"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "karaoke"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "dance"}, {"LOWER": "floor"}]},

            # Dietary Options
            {"label": "FEATURE", "pattern": [{"LOWER": "vegetarian"}, {"LOWER": "options"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "vegan"}, {"LOWER": "options"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "gluten-free"}, {"LOWER": "options"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "halal"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "kosher"}]},

            # Accessibility and Facilities
            {"label": "FEATURE", "pattern": [{"LOWER": "parking"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "free"}, {"LOWER": "parking"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "valet"}, {"LOWER": "parking"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "private"}, {"LOWER": "dining"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "wifi"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "free"}, {"LOWER": "wifi"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "charging"}, {"LOWER": "stations"}]},

            # Child-Friendly Features
            {"label": "FEATURE", "pattern": [{"LOWER": "highchairs"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "kids"}, {"LOWER": "menu"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "play"}, {"LOWER": "area"}]},

            # Alcohol and Bar Features
            {"label": "FEATURE", "pattern": [{"LOWER": "wine"}, {"LOWER": "list"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "craft"}, {"LOWER": "beer"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "cocktails"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "beer"}, {"LOWER": "on"}, {"LOWER": "tap"}]},

            # Business Features
            {"label": "FEATURE", "pattern": [{"LOWER": "business"}, {"LOWER": "meetings"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "conference"}, {"LOWER": "room"}]},
            {"label": "FEATURE", "pattern": [{"LOWER": "free"}, {"LOWER": "wifi"}, {"LOWER": "for"}, {"LOWER": "work"}]},

            {"label": "RATING", "pattern": [{"LOWER": "5"}, {"LOWER": "stars"}]},
            {"label": "RATING", "pattern": [{"LOWER": "4"}, {"LOWER": "stars"}]},
            {"label": "RATING", "pattern": [{"LOWER": "high"}, {"LOWER": "rated"}]},
            {"label": "RATING", "pattern": [{"LOWER": "top"}, {"LOWER": "rated"}]},
            {"label": "RATING", "pattern": [{"LOWER": "customer"}, {"LOWER": "reviews"}]}
        ]
        ruler.add_patterns(patterns)

        # Path to save the EntityRuler
        save_path = "/data/data/com.restaurantrecommender/files/entity_ruler_patterns"

        # Save the EntityRuler to disk
        ruler.to_disk(save_path)
        print(f"PYTHON EntityRuler saved successfully to: {save_path}")
    except Exception as e:
        print(f"PYTHON Error creating EntityRuler: {e}")
