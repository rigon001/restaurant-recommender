import spacy
from spacy.pipeline import EntityRuler
from pathlib import Path
import shutil

def load_entity_ruler():
    """
    Load the SpaCy model with the EntityRuler.
    """
    nlp = spacy.load("en_core_web_sm")
    internal_path = Path("/data/data/com.restaurantrecommender/files/entity_ruler_patterns")
    if not internal_path.exists():
        raise FileNotFoundError(f"PYTHON EntityRuler patterns not found at {internal_path}")
    ruler = EntityRuler(nlp).from_disk(internal_path)
    nlp.add_pipe(ruler, before="ner")
    return nlp

def process_sentence(sentence):
    """
    Process a sentence using the SpaCy model and return detected entities.
    :param sentence: str - The input sentence to process.
    :return: list of tuples (entity text, entity label)
    """
    nlp = load_entity_ruler()
    doc = nlp(sentence)
    return [(ent.text, ent.label_) for ent in doc.ents]
