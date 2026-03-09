// ============= TGDD Clone v2024 JS =============

$(document).ready(function () {

    function formatPrice(price) {
        var value = Number(price || 0);
        return value.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.') + ' ₫';
    }

    function buildProductCard(p) {
        var imgSrc = p.image || 'https://placehold.co/207x207/png?text=No+Image';
        var originalPrice = '';
        var promoHtml = '';

        if (p.discount > 0) {
            var orig = Math.round(p.price * 100 / (100 - p.discount));
            originalPrice = '<span class="price-and-discount">' +
                '<label class="price-old black">' + formatPrice(orig) + '</label>' +
                '<small>-' + Math.round(p.discount) + '%</small>' +
                '</span>';
        }

        var promoQty = Number(p.promotionQuantity || 0);
        if (p.promotional && promoQty > 0) {
            var promoPercent = Math.max(12, Math.min(100, promoQty * 5));
            promoHtml = '<div class="fs-contain">' +
                '<img width="15" height="15" src="//cdnv2.tgdd.vn/webmwg/2024/ContentMwg/images/homev2/flash-sale.png" alt="icon">' +
                '<span class="rq_count fscount"><i style="width:' + promoPercent + '%;" class="fs-iconfire"></i><b>Còn ' + promoQty + ' suất</b></span>' +
                '</div>';
        }

        return '<div class="item" data-id="' + p.id + '" data-category-id="' + (p.categoryId || 0) + '">' +
            '<a href="/products/' + p.id + '" class="remain_quantity main-contain">' +
            '<div class="item-img"><img src="' + imgSrc + '" alt="' + (p.name || '') + '"></div>' +
            '<h3>' + (p.name || '') + '</h3>' +
            '<strong class="price"><span>' + formatPrice(p.price) + '</span>' + originalPrice + '</strong>' +
            promoHtml +
            '</a>' +
            '<div class="btn-buy">' +
            '<a href="/products/' + p.id + '" class="see-detail hide">Xem chi tiết</a>' +
            '<a href="/cart/add?productId=' + p.id + '&quantity=1" class="buy-now">Mua ngay</a>' +
            '</div>' +
            '</div>';
    }

    var $productList = $('.listproduct.slider-flashsale');
    var allProductsLoaded = false;

    $('.products-tabs li').on('click', function (e) {
        if ($(this).closest('#homePromoTabs').length) {
            return;
        }
        e.preventDefault();
        e.stopPropagation();

        var link = $(this).find('a').attr('href');
        if (link && link !== 'javascript:;' && link !== '#') {
            window.location.href = link;
            return;
        }

        $('.products-tabs li').removeClass('active-tab');
        $(this).addClass('active-tab');

        var categoryId = $(this).data('category-id');
        if (categoryId === undefined || categoryId === null) {
            showAllItems();
            return;
        }
        filterByCategory(categoryId);
    });

    function showAllItems() {
        $productList.find('.item').show();
    }

    function filterByCategory(categoryId) {
        var $items = $productList.find('.item');
        var hasMatch = false;

        $items.each(function () {
            var itemCatId = parseInt($(this).data('category-id')) || 0;
            if (itemCatId === parseInt(categoryId)) {
                $(this).show();
                hasMatch = true;
            } else {
                $(this).hide();
            }
        });

        if (!hasMatch) {
            $.getJSON('/api/products', {categoryId: categoryId}, function (data) {
                if (data && data.length > 0) {
                    var html = '';
                    data.forEach(function (p) {
                        html += buildProductCard(p);
                    });
                    $productList.html(html);
                } else {
                    $productList.html('<div style="text-align:center;padding:40px;color:#999;grid-column:1/-1;">Không có sản phẩm nào trong danh mục này</div>');
                }
            });
        }
    }

    $('#viewall-products').on('click', function (e) {
        e.preventDefault();
        if (allProductsLoaded) {
            showAllItems();
            $('.products-tabs li').removeClass('active-tab');
            $('.products-tabs li:first').addClass('active-tab');
            return;
        }

        $.getJSON('/api/products', function (data) {
            if (data && data.length > 0) {
                var html = '';
                data.forEach(function (p) {
                    html += buildProductCard(p);
                });
                $productList.html(html);
                allProductsLoaded = true;
                $('.products-tabs li').removeClass('active-tab');
                $('.products-tabs li:first').addClass('active-tab');
            }
        });
    });

    var $menuHaslist = $('.main-menu > li.has-list');
    $menuHaslist.on('mouseenter', function () {
        $(this).find('.navmwg').css('display', 'flex');
        $('.header-mask').addClass('active');
    });
    $menuHaslist.on('mouseleave', function () {
        $(this).find('.navmwg').css('display', '');
        $('.header-mask').removeClass('active');
    });

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

    $('.listing-timeline a').on('click', function (e) {
        e.preventDefault();
        $('.listing-timeline a').removeClass('active');
        $(this).addClass('active');
    });

    $('.icon-close-popup').on('click', function () {
        $(this).closest('.popup-banner').fadeOut(300);
    });

    $('.close-bigBanner').on('click', function () {
        $(this).closest('.big-banner').slideUp(300);
    });

    $('.header__search .input-search').on('focus', function () {
        $(this).closest('.header__search').addClass('active');
    });

    $('.header__search .input-search').on('blur', function () {
        setTimeout(function () {
            $('.header__search').removeClass('active');
        }, 200);
    });

    $('.header-overlay a').on('click', function (e) {
        e.preventDefault();
        $('header.header').attr('data-sub', '0');
        $('.header-overlay').hide();
    });

    var $bannerItem = $('.banner-media .media-slider .item');
    if ($bannerItem.length && $bannerItem.data('background-color')) {
        $('.banner-media').css('background-color', $bannerItem.data('background-color'));
    }

    console.log('TGDD Clone v2024 loaded.');
});
