import spacy
from spacy.pipeline import EntityRuler
from pathlib import Path
import shutil

def load_entity_ruler(entity_ruler_path):
    """
    Load the SpaCy model and attach the EntityRuler from disk.
    """
    try:
        nlp = spacy.load("en_core_web_sm", disable=["parser", "tagger", "lemmatizer", "ner"])
        # Load EntityRuler patterns
        ruler = EntityRuler(nlp).from_disk(entity_ruler_path)
        nlp.add_pipe(ruler)
        print("SpaCy model and EntityRuler loaded successfully.")
        return nlp
    except Exception as e:
        print(f"Error loading SpaCy model or EntityRuler: {e}")
        raise

def process_sentence(sentence, entity_ruler_path):
    """
    Process a sentence using the SpaCy model and return detected entities.
    :param sentence: str - The input sentence to process.
    :return: list of tuples (entity text, entity label)
    """
    nlp = load_entity_ruler(entity_ruler_path)
    doc = nlp(sentence)
    return [(ent.text, ent.label_) for ent in doc.ents]
