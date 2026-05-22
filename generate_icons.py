"""
Generate Android launcher icon PNGs for all mipmap densities.
Creates a purple circle with a white ₹ symbol on a dark background.
"""
import struct
import zlib
import os
import math

def create_png_rgba(width, height, pixels):
    """Create a PNG file from RGBA pixel data."""
    def chunk(chunk_type, data):
        c = chunk_type + data
        crc = struct.pack('>I', zlib.crc32(c) & 0xffffffff)
        return struct.pack('>I', len(data)) + c + crc

    sig = b'\x89PNG\r\n\x1a\n'
    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)  # 6 = RGBA

    raw = b''
    for y in range(height):
        raw += b'\x00'  # filter: none
        for x in range(width):
            idx = (y * width + x) * 4
            raw += bytes(pixels[idx:idx+4])

    compressed = zlib.compress(raw, 9)
    return sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', compressed) + chunk(b'IEND', b'')


def draw_icon(size):
    """Draw a purple circle with ₹ on dark background, returns RGBA pixel list."""
    pixels = [0] * (size * size * 4)

    cx, cy = size / 2.0, size / 2.0
    radius = size * 0.42

    # Background: #0F0F0F, Circle: #6C63FF
    for y in range(size):
        for x in range(size):
            idx = (y * size + x) * 4
            dist = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)

            if dist <= radius:
                # Purple circle
                pixels[idx] = 108     # R
                pixels[idx+1] = 99    # G
                pixels[idx+2] = 255   # B
                pixels[idx+3] = 255   # A
            else:
                # Dark background
                pixels[idx] = 15
                pixels[idx+1] = 15
                pixels[idx+2] = 15
                pixels[idx+3] = 255

    # Draw a simple white ₹ symbol using basic line drawing
    # We'll draw horizontal bars and a curved stroke
    white = (255, 255, 255, 255)
    
    def set_pixel(px, py):
        if 0 <= px < size and 0 <= py < size:
            idx = (py * size + px) * 4
            pixels[idx], pixels[idx+1], pixels[idx+2], pixels[idx+3] = white

    def draw_thick_hline(y, x1, x2, thickness=2):
        t = max(1, int(thickness))
        for dy in range(-t//2, t//2 + 1):
            for x in range(int(x1), int(x2)+1):
                set_pixel(x, int(y) + dy)

    def draw_thick_line(x1, y1, x2, y2, thickness=2):
        t = max(1, int(thickness))
        steps = max(abs(int(x2-x1)), abs(int(y2-y1)), 1) * 2
        for i in range(steps + 1):
            frac = i / steps
            px = int(x1 + (x2 - x1) * frac)
            py = int(y1 + (y2 - y1) * frac)
            for dx in range(-t//2, t//2 + 1):
                for dy in range(-t//2, t//2 + 1):
                    set_pixel(px + dx, py + dy)

    # Scale factor
    s = size / 48.0
    thick = max(2, int(2 * s))

    # Top horizontal bar
    draw_thick_hline(int(16 * s), int(15 * s), int(33 * s), thick)
    # Second horizontal bar
    draw_thick_hline(int(22 * s), int(15 * s), int(33 * s), thick)
    # Vertical stroke (left side)
    draw_thick_line(int(17 * s), int(16 * s), int(17 * s), int(36 * s), thick)
    # Diagonal stroke (the slant of ₹)
    draw_thick_line(int(22 * s), int(22 * s), int(33 * s), int(36 * s), thick)

    return pixels


def main():
    base = os.path.join(
        'c:', os.sep, 'Users', 'murali.aravelli', 'DATA',
        'Finance-Tracker', 'UPIExpenseTracker', 'app', 'src', 'main', 'res'
    )

    densities = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192,
    }

    for folder_name, size in densities.items():
        folder_path = os.path.join(base, folder_name)
        os.makedirs(folder_path, exist_ok=True)

        pixels = draw_icon(size)
        png_data = create_png_rgba(size, size, pixels)

        for icon_name in ['ic_launcher.png', 'ic_launcher_round.png']:
            file_path = os.path.join(folder_path, icon_name)
            with open(file_path, 'wb') as f:
                f.write(png_data)

        print(f'Created {folder_name} ({size}x{size})')

    print('\nAll mipmap launcher icons created successfully!')
    print('Now commit and push to GitHub to trigger a new build.')


if __name__ == '__main__':
    main()
