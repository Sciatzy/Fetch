import glob, os

layout_dir = '/Users/arcelmacasling/Desktop/FETCH/Fetch/app/src/main/res/layout'
for fpath in glob.glob(os.path.join(layout_dir, '*.xml')):
    with open(fpath, 'r') as f:
        data = f.read()

    data = data.replace('android:background="@drawable/bg_glass_card"', '')
    data = data.replace('app:cardBackgroundColor="@android:color/transparent"', 'app:cardBackgroundColor="#E6FFFFFF"')
    data = data.replace('app:strokeWidth="0dp"', 'app:strokeWidth="1dp"')
    data = data.replace('app:strokeColor="@android:color/transparent"', 'app:strokeColor="#FFFFFF"')
    data = data.replace('app:cardElevation="16dp"', 'app:cardElevation="0dp"')

    # Clean up empty background attributes that might have been left over
    data = data.replace('\n        \n', '\n')
    
    with open(fpath, 'w') as f:
        f.write(data)
