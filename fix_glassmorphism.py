import os
import glob

LAYOUT_DIR = 'app/src/main/res/layout'

REPLACEMENTS = {
    # Colors
    '"@color/black"': '"@color/text_primary"',
    '"@color/pure_black"': '"@color/text_primary"',
    '"@color/brand_ink"': '"@color/text_primary"',
    '"@color/text_grey"': '"@color/text_secondary"',
    '"@color/white"': '"@color/glass_white"',
    '"#212121"': '"@color/text_primary"',
    '"#757575"': '"@color/text_secondary"',
    '"#FFFFFF"': '"@color/glass_white"',
    
    # Backgrounds
    '"@color/bg_gray"': '"@drawable/bg_screen_gradient"',
    '"@drawable/bg_input_rounded"': '"@drawable/bg_input_field"',
    '"@drawable/bg_bottom_nav"': '"@drawable/bg_glass_card"',
    '"@drawable/bg_card_default"': '"@drawable/bg_glass_card"',

    # Material Cards Surface
    'app:cardBackgroundColor="@color/white"': 'app:cardBackgroundColor="@color/glass_white_alpha"\n        app:strokeWidth="1dp"\n        app:strokeColor="@color/glass_border"\n        app:cardElevation="0dp"',
    'app:cardBackgroundColor="@color/glass_white"': 'app:cardBackgroundColor="@color/glass_white_alpha"\n        app:strokeWidth="1dp"\n        app:strokeColor="@color/glass_border"\n        app:cardElevation="0dp"',
    'app:cardBackgroundColor="#FFFFFF"': 'app:cardBackgroundColor="@color/glass_white_alpha"\n        app:strokeWidth="1dp"\n        app:strokeColor="@color/glass_border"\n        app:cardElevation="0dp"',
    
    # Button Colors to Ghost / Primary
    'android:background="@color/brand_red"': 'android:background="@drawable/bg_button_primary"',
    'android:background="@drawable/bg_button_secondary"': 'android:background="@drawable/bg_button_ghost"',
    'android:textColor="@android:color/white"': 'android:textColor="@color/text_on_red"',
    'android:textColor="@color/brand_red"': 'android:textColor="@color/glass_red"',
}

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content
    for old, new in REPLACEMENTS.items():
        content = content.replace(old, new)
        
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Fixed {filepath}")

if __name__ == "__main__":
    for filepath in glob.glob(f"{LAYOUT_DIR}/**/*.xml", recursive=True):
        process_file(filepath)
