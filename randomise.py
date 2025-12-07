import json
import random

# Input + output paths
INPUT_FILE = r"C:\Users\HP\Desktop\mini project1\dictionary.json"
OUTPUT_FILE = r"C:\Users\HP\Desktop\mini project1\dictionaryshuffled.json"

def shuffle_dataset(input_path, output_path):
    # Load the JSON file
    with open(input_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # Check that it's theme -> list structure
    if not isinstance(data, dict):
        raise ValueError("Expected a dictionary at the root of the JSON file")

    # Shuffle each theme independently
    for theme, words in data.items():
        if isinstance(words, list):
            random.shuffle(words)  # in-place shuffle
        else:
            raise ValueError(f"Theme '{theme}' does not contain a list")

    # Save the shuffled dataset
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)



# Run
if __name__ == "__main__":
    shuffle_dataset(INPUT_FILE, OUTPUT_FILE)
