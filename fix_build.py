import glob
import re

LAYOUT_DIR = 'app/src/main/res/layout'

def fix_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Upgrade CardView to MaterialCardView
    content = content.replace("<androidx.cardview.widget.CardView", "<com.google.android.material.card.MaterialCardView")
    content = content.replace("</androidx.cardview.widget.CardView>", "</com.google.android.material.card.MaterialCardView>")

    # Remove duplicate app:cardElevation where we just injected app:cardElevation="0dp"
    # To be safe, let's just strip any line matching `app:cardElevation="Xdp"` that comes *after* our 0dp block
    # Actually, a safer way via regex:
    # If a tag has multiple cardElevation attributes, keep only the first one or simply strip the \d+dp one if it's there
    # But an easier hack is to find 'app:cardElevation="[1-9][0-9]*dp"' and remove it IF there's already a '0dp' nearby
    # Since XML attributes can be scattered, let's just use regex to remove ALL cardElevation lines except '0dp'
    # Actually, no, some cards might want genuine elevation. We only care where we injected:
    # 'app:cardBackgroundColor="@color/glass_white_alpha"\n        app:strokeWidth="1dp"\n        app:strokeColor="@color/glass_border"\n        app:cardElevation="0dp"'
    
    # A generic fix: if "app:cardElevation="0dp"" is in the file, we should remove nearby "app:cardElevation="...""
    # Better: just use `re.sub` to remove the line with `app:cardElevation="3dp"` etc. 
    # Let's just find and remove duplicate attributes in the whole XML string.
    # An attribute duplicate error happens per tag.
    
    # For now, let's just remove ALL older cardElevations
    # Actually, I can just use a simple regex to replace `app:cardElevation="\d+dp"` with `""` ONLY IF it's not `0dp`.
    # Wait, the prompt specifically wanted glass cards to be Flat (0dp) OR to have 4dp-8dp.
    # Let's just blindly clean up `app:cardElevation="3dp"`, etc., in the same tag.
    # I will just write a simple parsing loop or use regex.
    blocks = re.findall(r'<com\.google\.android\.material\.card\.MaterialCardView.*?>', content, flags=re.DOTALL)
    for block in blocks:
        # if the block has more than one `cardElevation`, remove the one that is NOT 0dp
        if block.count('app:cardElevation') > 1:
            # find all cardElevations
            cleaned_block = re.sub(r'\s+app:cardElevation="[1-9][0-9]*dp"', '', block)
            content = content.replace(block, cleaned_block)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    for filepath in glob.glob(f"{LAYOUT_DIR}/**/*.xml", recursive=True):
        fix_file(filepath)
