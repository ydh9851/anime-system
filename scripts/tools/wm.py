import cv2, numpy as np
for f in ["login-bg.png", "dashboard-hero.png"]:
    p = "frontend/src/assets/" + f
    im = cv2.imread(p)
    h, w = im.shape[:2]
    m = np.zeros((h, w), np.uint8)
    m[int(h*.90):, int(w*.74):] = 255
    out = cv2.inpaint(im, m, 7, cv2.INPAINT_TELEA)
    cv2.imwrite(p, out)
    print("ok", f)
