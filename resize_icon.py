import os
from PIL import Image

# مسیر عکس اصلی (اسم فایلت رو اینجا بذار)
SOURCE_IMAGE = "icon.png"

# مسیر پوشه‌های mipmap توی پروژه
RES_DIR = "app/src/main/res"

# سایزهای استاندارد اندروید برای هر پوشه
SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def main():
    if not os.path.exists(SOURCE_IMAGE):
        print(f"Error: {SOURCE_IMAGE} not found in the current directory.")
        return

    # باز کردن عکس اصلی
    img = Image.open(SOURCE_IMAGE).convert("RGBA")

    for folder, size in SIZES.items():
        # ساخت مسیر پوشه (اگه نبود، می‌سازه)
        folder_path = os.path.join(RES_DIR, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # تغییر سایز عکس
        resized_img = img.resize((size, size), Image.LANCZOS)
        
        # ذخیره به اسم ic_launcher.png
        output_path = os.path.join(folder_path, "ic_launcher.png")
        resized_img.save(output_path, "PNG")
        print(f"Saved: {output_path} ({size}x{size})")

        # ذخیره به اسم ic_launcher_round.png (برای گوشی‌هایی که آیکون گرد می‌خوان)
        round_output_path = os.path.join(folder_path, "ic_launcher_round.png")
        resized_img.save(round_output_path, "PNG")
        print(f"Saved: {round_output_path} ({size}x{size})")

    print("Done!")

if __name__ == "__main__":
    main()