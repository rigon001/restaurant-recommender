import os
import spacy

def load_spacy_model():
    """
    Load the SpaCy model installed via Chaquopy.
    """
    try:
        # Load the model directly by name
        nlp = spacy.load("en_core_web_sm")
        print("SpaCy model loaded successfully.")
        return nlp
    except Exception as e:
        print(f"Error loading SpaCy model: {e}")
        raise


def extract_focus_terms(query, nlp):
    """
    Extract focus terms (nouns, adjectives, proper nouns) from the query.

    :param query: str - Input query.
    :param nlp: spacy.Language - SpaCy NLP model.
    :return: list of str - List of focus terms.
    """
    doc = nlp(query)
    return [token.lemma_ for token in doc if token.pos_ in ["NOUN", "ADJ", "PROPN"]]


def enhance_query(query_terms, styles, prices, previous_queries, nlp):
    """
    Enhance the query by adding relevant terms from styles, prices, and previous queries.

    :param query_terms: list of str - Focus terms extracted from the initial query.
    :param styles: Java ArrayList - Preferred restaurant styles.
    :param prices: Java ArrayList - Preferred price ranges.
    :param previous_queries: Java ArrayList - User's previous search queries.
    :param nlp: spacy.Language - SpaCy NLP model.
    :return: str - The enhanced query.
    """
    # Convert Java ArrayList to Python list using .toArray()
    styles = list(styles.toArray())
    prices = list(prices.toArray())
    previous_queries = list(previous_queries.toArray())

    enhanced_query = set(query_terms)

    # Add styles and prices to the query
    enhanced_query.update(styles)
    enhanced_query.update(prices)

    # Add focus terms from previous queries
    for prev_query in previous_queries:
        prev_focus_terms = extract_focus_terms(prev_query, nlp)
        enhanced_query.update(prev_focus_terms)

    return " ".join(enhanced_query)


def complement_query(initial_query, styles, prices, previous_queries):
    """
    Refine the user's initial query by incorporating their preferences.

    :param initial_query: str - The user's initial search query.
    :param styles: list of str - Preferred restaurant styles.
    :param prices: list of str - Preferred price ranges.
    :param previous_queries: list of str - User's previous search queries.
    :return: str - The refined search query.
    """
    # Load the SpaCy model
    nlp = load_spacy_model()

    # Extract focus terms from the initial query
    focus_terms = extract_focus_terms(initial_query, nlp)
    print(f"PYTHON SpaCy version: {spacy.__version__}")
    print(f"PYTHON focus_terms: {focus_terms}")

    # Enhance the query with user preferences
    enhanced_query = enhance_query(focus_terms, styles, prices, previous_queries, nlp)
    print(f"PYTHON enhanced_query: {enhanced_query}")

    return enhanced_query
