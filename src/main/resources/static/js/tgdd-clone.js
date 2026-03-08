// ============= TGDD Clone v2024 JS =============
// Matches thegioididong.com home page interactions

$(document).ready(function() {

    // ===== FORMAT PRICE HELPER =====
    function formatPrice(price) {
        return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.') + ' ₫';
    }

    // ===== BUILD PRODUCT CARD HTML =====
    function buildProductCard(p) {
        var imgSrc = p.image || 'https://placehold.co/207x207/png?text=No+Image';
        var originalPrice = '';
        if (p.discount > 0) {
            var orig = Math.round(p.price * 100 / (100 - p.discount));
            originalPrice = '<span class="price-and-discount">' +
                '<label class="price-old black">' + formatPrice(orig) + '</label>' +
                '<small>-' + Math.round(p.discount) + '%</small>' +
                '</span>';
        }
        return '<div class="item" data-id="' + p.id + '" data-category-id="' + (p.categoryId || 0) + '">' +
            '<a href="/products/' + p.id + '" class="remain_quantity main-contain">' +
                '<div class="item-img"><img src="' + imgSrc + '" alt="' + (p.name || '') + '"></div>' +
                '<h3>' + (p.name || '') + '</h3>' +
                '<strong class="price"><span>' + formatPrice(p.price) + '</span>' + originalPrice + '</strong>' +
                '<div class="fs-contain">' +
                    '<img width="15" height="15" src="//cdnv2.tgdd.vn/webmwg/2024/ContentMwg/images/homev2/flash-sale.png" alt="icon">' +
                    '<span class="rq_count fscount"><i style="width:65%;" class="fs-iconfire"></i><b>Đang bán chạy</b></span>' +
                '</div>' +
            '</a>' +
            '<div class="btn-buy">' +
                '<a href="/products/' + p.id + '" class="see-detail hide">Xem chi tiết</a>' +
                '<a href="/cart/add?productId=' + p.id + '&quantity=1" class="buy-now">Mua ngay</a>' +
            '</div>' +
        '</div>';
    }

    // ===== PRODUCT TABS SWITCHING WITH AJAX =====
    var $productList = $('.listproduct.slider-flashsale');
    var allProductsLoaded = false;  // track if "view all" has been clicked

    $('.products-tabs li').on('click', function(e) {
        e.preventDefault();
        e.stopPropagation();

        // Update tab state
        $('.products-tabs li').removeClass('active-tab');
        $(this).addClass('active-tab');

        var categoryId = $(this).data('category-id');

        // If it's the fixed banner tabs (no category-id), show all products
        if (categoryId === undefined || categoryId === null) {
            showAllItems();
            return;
        }

        // Filter by category via AJAX
        filterByCategory(categoryId);
    });

    function showAllItems() {
        $productList.find('.item').show();
    }

    function filterByCategory(categoryId) {
        // First try client-side filtering
        var $items = $productList.find('.item');
        var hasMatch = false;

        $items.each(function() {
            var itemCatId = parseInt($(this).data('category-id')) || 0;
            if (itemCatId === parseInt(categoryId)) {
                $(this).show();
                hasMatch = true;
            } else {
                $(this).hide();
            }
        });

        // If no match found client-side, fetch from server
        if (!hasMatch) {
            $.getJSON('/api/products', { categoryId: categoryId }, function(data) {
                if (data && data.length > 0) {
                    var html = '';
                    data.forEach(function(p) {
                        html += buildProductCard(p);
                    });
                    $productList.html(html);
                } else {
                    $productList.html('<div style="text-align:center;padding:40px;color:#999;grid-column:1/-1;">Không có sản phẩm nào trong danh mục này</div>');
                }
            });
        }
    }

    // ===== VIEW ALL PRODUCTS =====
    $('#viewall-products').on('click', function(e) {
        e.preventDefault();
        if (allProductsLoaded) {
            // Already loaded - just show all and reset tab
            showAllItems();
            $('.products-tabs li').removeClass('active-tab');
            $('.products-tabs li:first').addClass('active-tab');
            return;
        }

        // Fetch ALL products from server
        $.getJSON('/api/products', function(data) {
            if (data && data.length > 0) {
                var html = '';
                data.forEach(function(p) {
                    html += buildProductCard(p);
                });
                $productList.html(html);
                allProductsLoaded = true;
                // Reset tab
                $('.products-tabs li').removeClass('active-tab');
                $('.products-tabs li:first').addClass('active-tab');
            }
        });
    });

    // ===== MAIN MENU DROPDOWN (hover - exact TGDD behavior) =====
    var $menuHaslist = $('.main-menu > li.has-list');
    $menuHaslist.on('mouseenter', function() {
        $(this).find('.navmwg').css('display', 'flex');
        $('.header-mask').addClass('active');
    });
    $menuHaslist.on('mouseleave', function() {
        $(this).find('.navmwg').css('display', '');
        $('.header-mask').removeClass('active');
    });

    // ===== FLASH SALE COUNTDOWN TIMER =====
    function updateCountdown() {
        var now = new Date();
        var endOfDay = new Date();
        endOfDay.setHours(23, 59, 59, 999);
        var diff = endOfDay - now;
        if (diff <= 0) return;
        var h = Math.floor(diff / 3600000);
        var m = Math.floor((diff % 3600000) / 60000);
        var s = Math.floor((diff % 60000) / 1000);
        $('#hour').text(String(h).padStart(2, '0'));
        $('#minute').text(String(m).padStart(2, '0'));
        $('#second').text(String(s).padStart(2, '0'));
    }
    updateCountdown();
    setInterval(updateCountdown, 1000);

    // ===== FLASH SALE TIMELINE TABS =====
    $('.listing-timeline a').on('click', function(e) {
        e.preventDefault();
        $('.listing-timeline a').removeClass('active');
        $(this).addClass('active');
    });

    // ===== POPUP BANNER close =====
    $('.icon-close-popup').on('click', function() {
        $(this).closest('.popup-banner').fadeOut(300);
    });

    // ===== BIG BANNER close =====
    $('.close-bigBanner').on('click', function() {
        $(this).closest('.big-banner').slideUp(300);
    });

    // ===== HEADER SEARCH FOCUS (exact TGDD) =====
    $('.header__search .input-search').on('focus', function() {
        $(this).closest('.header__search').addClass('active');
    });
    $('.header__search .input-search').on('blur', function() {
        setTimeout(function() {
            $('.header__search').removeClass('active');
        }, 200);
    });

    // ===== HEADER-OVERLAY close =====
    $('.header-overlay a').on('click', function(e) {
        e.preventDefault();
        $('header.header').attr('data-sub', '0');
        $('.header-overlay').hide();
    });

    // ===== BANNER TOP BAR background color =====
    var $bannerItem = $('.banner-media .media-slider .item');
    if ($bannerItem.length && $bannerItem.data('background-color')) {
        $('.banner-media').css('background-color', $bannerItem.data('background-color'));
    }

    console.log('TGDD Clone v2024 loaded.');
});
