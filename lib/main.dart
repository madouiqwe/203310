import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';
import 'package:share_plus/share_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';

/* ═══════════════════════════════════════════════════════════════
   ⚙️  قسم التعديل السريع
   ═══════════════════════════════════════════════════════════════ */

class AppConfig {
  static const int defaultColorIndex = 0;
  static const String defaultThemeMode = 'light';
  static const String defaultFont = 'Cairo';
  static const String companyName = 'اسم شركتك';
  static const String companyPhone = '0555 55 55 55';
  static const String companyAddress = 'العنوان';
  static const String currencySymbol = 'د.ج';
  static const String numberLocale = 'en';
  static const String dateFormat = 'yyyy/MM/dd';
  static const String appTitle = 'فاتورة سريعة';
}

/* ═══════════════════════════════════════════════════════════════ */

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('ar', null);
  Intl.defaultLocale = 'ar';
  await AppSettings.load();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: AppSettings.instance,
      builder: (context, _) {
        final s = AppSettings.instance;
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          title: AppConfig.appTitle,
          themeMode: s.themeMode,
          theme: _buildTheme(ColorScheme.fromSeed(
              seedColor: s.primaryColor, brightness: Brightness.light)),
          darkTheme: _buildTheme(ColorScheme.fromSeed(
              seedColor: s.primaryColor, brightness: Brightness.dark)),
          home: const HomeScreen(),
        );
      },
    );
  }

  ThemeData _buildTheme(ColorScheme scheme) {
    final base = ThemeData(useMaterial3: true, colorScheme: scheme);
    
    // ✅ الإصلاح: تطبيق الخط بدون تعارض مع material_ui
    TextTheme appliedTextTheme;
    switch (AppSettings.instance.fontFamily) {
      case 'Tajawal':
        appliedTextTheme = base.textTheme.apply(fontFamily: GoogleFonts.tajawal().fontFamily);
        break;
      case 'Amiri':
        appliedTextTheme = base.textTheme.apply(fontFamily: GoogleFonts.amiri().fontFamily);
        break;
      case 'Almarai':
        appliedTextTheme = base.textTheme.apply(fontFamily: GoogleFonts.almarai().fontFamily);
        break;
      case 'Noto Naskh Arabic':
        appliedTextTheme = base.textTheme.apply(fontFamily: GoogleFonts.notoNaskhArabic().fontFamily);
        break;
      default:
        appliedTextTheme = base.textTheme.apply(fontFamily: GoogleFonts.cairo().fontFamily);
        break;
    }

    return base.copyWith(
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(),
        isDense: true,
      ),
      textTheme: appliedTextTheme,
      appBarTheme: AppBarTheme(
        backgroundColor: scheme.primaryContainer,
        foregroundColor: scheme.onPrimaryContainer,
      ),
    );
  }
}

/* ========================= الإعدادات ========================= */

class AppSettings extends ChangeNotifier {
  static final AppSettings instance = AppSettings._();
  AppSettings._();

  static const _kColor = 'theme_color';
  static const _kMode = 'theme_mode';
  static const _kFont = 'font_family';
  static const _kCompany = 'company_name';
  static const _kPhone = 'company_phone';
  static const _kAddress = 'company_address';

  int _colorIndex = AppConfig.defaultColorIndex;
  ThemeMode _themeMode = ThemeMode.light;
  String _fontFamily = AppConfig.defaultFont;
  String _companyName = AppConfig.companyName;
  String _companyPhone = AppConfig.companyPhone;
  String _companyAddress = AppConfig.companyAddress;

  int get colorIndex => _colorIndex;
  ThemeMode get themeMode => _themeMode;
  String get fontFamily => _fontFamily;
  String get companyName => _companyName;
  String get companyPhone => _companyPhone;
  String get companyAddress => _companyAddress;
  Color get primaryColor => themeColors[_colorIndex];

  static const List<Color> themeColors = [
    Colors.teal, Colors.blue, Colors.purple, Colors.green,
    Colors.orange, Colors.red, Colors.indigo,
  ];

  static const List<String> themeColorNames = [
    'فيروزي', 'أزرق', 'بنفسجي', 'أخضر', 'برتقالي', 'أحمر', 'نيلي',
  ];

  static const List<String> fontOptions = [
    'Cairo', 'Tajawal', 'Amiri', 'Almarai', 'Noto Naskh Arabic',
  ];

  static const Map<String, String> fontNamesAr = {
    'Cairo': 'القاهرة', 'Tajawal': 'تجوال', 'Amiri': 'أميري',
    'Almarai': 'المراعي', 'Noto Naskh Arabic': 'نسخ',
  };

  static Future<void> load() async {
    final p = await SharedPreferences.getInstance();
    instance._colorIndex = p.getInt(_kColor) ?? AppConfig.defaultColorIndex;
    final mode = p.getString(_kMode) ?? AppConfig.defaultThemeMode;
    instance._themeMode = mode == 'dark' ? ThemeMode.dark
        : mode == 'system' ? ThemeMode.system : ThemeMode.light;
    instance._fontFamily = p.getString(_kFont) ?? AppConfig.defaultFont;
    instance._companyName = p.getString(_kCompany) ?? AppConfig.companyName;
    instance._companyPhone = p.getString(_kPhone) ?? AppConfig.companyPhone;
    instance._companyAddress = p.getString(_kAddress) ?? AppConfig.companyAddress;
  }

  Future<void> setColor(int i) async {
    _colorIndex = i;
    (await SharedPreferences.getInstance()).setInt(_kColor, i);
    notifyListeners();
  }

  Future<void> setMode(ThemeMode m) async {
    _themeMode = m;
    final v = m == ThemeMode.dark ? 'dark'
        : m == ThemeMode.system ? 'system' : 'light';
    (await SharedPreferences.getInstance()).setString(_kMode, v);
    notifyListeners();
  }

  Future<void> setFont(String f) async {
    _fontFamily = f;
    (await SharedPreferences.getInstance()).setString(_kFont, f);
    notifyListeners();
  }

  Future<void> setCompany({String? name, String? phone, String? address}) async {
    final p = await SharedPreferences.getInstance();
    if (name != null) { _companyName = name; await p.setString(_kCompany, name); }
    if (phone != null) { _companyPhone = phone; await p.setString(_kPhone, phone); }
    if (address != null) { _companyAddress = address; await p.setString(_kAddress, address); }
    notifyListeners();
  }
}

/* ============================ النماذج ============================ */

class InvoiceItem {
  String name;
  double qty;
  double price;
  InvoiceItem({required this.name, required this.qty, required this.price});
  double get total => qty * price;
  Map<String, dynamic> toJson() => {'name': name, 'qty': qty, 'price': price};
  factory InvoiceItem.fromJson(Map<String, dynamic> j) => InvoiceItem(
    name: j['name'] as String? ?? '',
    qty: (j['qty'] as num?)?.toDouble() ?? 0,
    price: (j['price'] as num?)?.toDouble() ?? 0,
  );
  InvoiceItem copy() => InvoiceItem(name: name, qty: qty, price: price);
}

class Invoice {
  final String id;
  final String invoiceNumber;
  final String customer;
  final String date;
  final List<InvoiceItem> items;
  final double discount;
  final double taxRate;
  final String status;
  final int? deletedAt;

  Invoice({
    required this.id, required this.invoiceNumber, required this.customer,
    required this.date, required this.items, this.discount = 0,
    this.taxRate = 0, this.status = 'unpaid', this.deletedAt,
  });

  bool get isPaid => status == 'paid';
  bool get isDeleted => deletedAt != null;
  double get subtotal => items.fold(0.0, (sum, i) => sum + i.total);
  double get taxAmount => subtotal * (taxRate / 100);
  double get total => subtotal - discount + taxAmount;

  Map<String, dynamic> toJson() => {
    'id': id, 'invoiceNumber': invoiceNumber, 'customer': customer,
    'date': date, 'items': items.map((e) => e.toJson()).toList(),
    'discount': discount, 'taxRate': taxRate, 'status': status,
    'deletedAt': deletedAt,
  };

  factory Invoice.fromJson(Map<String, dynamic> j) => Invoice(
    id: j['id'] as String? ?? '',
    invoiceNumber: j['invoiceNumber'] as String? ?? '',
    customer: j['customer'] as String? ?? '',
    date: j['date'] as String? ?? '',
    items: (j['items'] as List<dynamic>? ?? [])
        .map((e) => InvoiceItem.fromJson(e as Map<String, dynamic>))
        .toList(),
    discount: (j['discount'] as num?)?.toDouble() ?? 0,
    taxRate: (j['taxRate'] as num?)?.toDouble() ?? 0,
    status: j['status'] as String? ?? 'unpaid',
    deletedAt: j['deletedAt'] as int?,
  );

  Invoice copyWith({String? id, String? invoiceNumber, String? status,
      int? deletedAt, bool clearDeletedAt = false}) => Invoice(
    id: id ?? this.id,
    invoiceNumber: invoiceNumber ?? this.invoiceNumber,
    customer: customer, date: date,
    items: items.map((e) => e.copy()).toList(),
    discount: discount, taxRate: taxRate,
    status: status ?? this.status,
    deletedAt: clearDeletedAt ? null : (deletedAt ?? this.deletedAt),
  );
}

class QuickItem {
  final String name;
  final double price;
  QuickItem({required this.name, required this.price});
  Map<String, dynamic> toJson() => {'name': name, 'price': price};
  factory QuickItem.fromJson(Map<String, dynamic> j) => QuickItem(
    name: j['name'] as String? ?? '',
    price: (j['price'] as num?)?.toDouble() ?? 0,
  );
}

class QuickItemsStore {
  static const _key = 'quick_items_v1';

  static Future<List<QuickItem>> load() async {
    try {
      final p = await SharedPreferences.getInstance();
      final raw = p.getString(_key);
      if (raw == null || raw.isEmpty) {
        final initial = [
          QuickItem(name: 'قهوة عربية', price: 450.0),
          QuickItem(name: 'قهوة تركية', price: 300.0),
          QuickItem(name: 'شاي أسود فاخر', price: 200.0),
          QuickItem(name: 'شاي أخضر بالنعناع', price: 250.0),
          QuickItem(name: 'سكر أبيض (1 كغ)', price: 110.0),
          QuickItem(name: 'زيت زيتون (1 لتر)', price: 1200.0),
          QuickItem(name: 'حليب معقم (1 لتر)', price: 130.0),
          QuickItem(name: 'تمر دقلة نور', price: 600.0),
          QuickItem(name: 'عسل طبيعي (500 غ)', price: 1800.0),
          QuickItem(name: 'مياه معدنية (1.5 لتر)', price: 50.0),
        ];
        await save(initial);
        return initial;
      }
      final decoded = jsonDecode(raw);
      if (decoded is! List) return [];
      return decoded.whereType<Map<String, dynamic>>()
          .map(QuickItem.fromJson).toList();
    } catch (_) { return []; }
  }

  static Future<void> save(List<QuickItem> items) async {
    final p = await SharedPreferences.getInstance();
    await p.setString(_key, jsonEncode(items.map((e) => e.toJson()).toList()));
  }

  static Future<void> add(QuickItem item) async {
    final list = await load();
    list.removeWhere((e) => e.name == item.name);
    list.add(item);
    if (list.length > 20) list.removeAt(0);
    await save(list);
  }

  static Future<void> remove(String name) async {
    final list = await load();
    list.removeWhere((e) => e.name == name);
    await save(list);
  }
}

class ProductStore {
  static const _key = 'products_v1';
  static Future<List<QuickItem>> load() async {
    try {
      final p = await SharedPreferences.getInstance();
      final raw = p.getString(_key);
      if (raw == null || raw.isEmpty) {
        final initial = [
          QuickItem(name: 'قهوة عربية', price: 450.0),
          QuickItem(name: 'قهوة تركية', price: 300.0),
          QuickItem(name: 'شاي أسود فاخر', price: 200.0),
          QuickItem(name: 'شاي أخضر بالنعناع', price: 250.0),
          QuickItem(name: 'سكر أبيض (1 كغ)', price: 110.0),
          QuickItem(name: 'زيت زيتون (1 لتر)', price: 1200.0),
          QuickItem(name: 'حليب معقم (1 لتر)', price: 130.0),
          QuickItem(name: 'تمر دقلة نور', price: 600.0),
          QuickItem(name: 'عسل طبيعي (500 غ)', price: 1800.0),
          QuickItem(name: 'مياه معدنية (1.5 لتر)', price: 50.0),
        ];
        await save(initial);
        return initial;
      }
      final d = jsonDecode(raw);
      if (d is! List) return [];
      return d.whereType<Map<String, dynamic>>().map(QuickItem.fromJson).toList();
    } catch (_) { return []; }
  }
  static Future<void> save(List<QuickItem> items) async {
    final p = await SharedPreferences.getInstance();
    await p.setString(_key, jsonEncode(items.map((e) => e.toJson()).toList()));
  }
  static Future<void> upsert(QuickItem item) async {
    final list = await load();
    list.removeWhere((e) => e.name.trim() == item.name.trim());
    list.insert(0, item);
    await save(list);
  }
  static Future<void> remove(String name) async {
    final list = await load();
    list.removeWhere((e) => e.name == name);
    await save(list);
  }
}

class InvoiceStore {
  static const String _key = 'invoices_v3';

  static Future<List<Invoice>> load() async {
    try {
      final p = await SharedPreferences.getInstance();
      final raw = p.getString(_key);
      if (raw == null || raw.isEmpty) return [];
      final decoded = jsonDecode(raw);
      if (decoded is! List) return [];
      return decoded.whereType<Map<String, dynamic>>()
          .map(Invoice.fromJson).toList();
    } catch (e) { debugPrint('خطأ: $e'); return []; }
  }

  static Future<void> saveAll(List<Invoice> list) async {
    final p = await SharedPreferences.getInstance();
    await p.setString(_key, jsonEncode(list.map((e) => e.toJson()).toList()));
  }

  static Future<void> add(Invoice inv) async {
    final list = await load();
    list.add(inv);
    await saveAll(list);
  }

  static Future<void> update(Invoice inv) async {
    final list = await load();
    final i = list.indexWhere((e) => e.id == inv.id);
    if (i != -1) list[i] = inv;
    await saveAll(list);
  }

  static Future<void> softDelete(String id) async {
    final list = await load();
    final i = list.indexWhere((e) => e.id == id);
    if (i != -1) list[i] = list[i].copyWith(
        deletedAt: DateTime.now().millisecondsSinceEpoch);
    await saveAll(list);
  }

  static Future<void> restore(String id) async {
    final list = await load();
    final i = list.indexWhere((e) => e.id == id);
    if (i != -1) list[i] = list[i].copyWith(clearDeletedAt: true);
    await saveAll(list);
  }

  static Future<void> permanentDelete(String id) async {
    final list = await load();
    list.removeWhere((e) => e.id == id);
    await saveAll(list);
  }

  static Future<void> emptyTrash() async {
    final list = await load();
    list.removeWhere((e) => e.isDeleted);
    await saveAll(list);
  }

  static Future<void> clear() async {
    (await SharedPreferences.getInstance()).remove(_key);
  }

  static Future<String> export() async {
    final list = await load();
    return jsonEncode({
      'version': 3,
      'exported_at': DateTime.now().toIso8601String(),
      'invoices': list.map((e) => e.toJson()).toList(),
    });
  }

  static Future<int> import(String jsonStr, {bool replace = false}) async {
    final data = jsonDecode(jsonStr);
    List<dynamic> raw;
    if (data is Map && data['invoices'] is List) {
      raw = data['invoices'] as List;
    } else if (data is List) {
      raw = data;
    } else { throw Exception('صيغة غير صحيحة'); }
    final imported = raw.whereType<Map<String, dynamic>>()
        .map(Invoice.fromJson).toList();
    final existing = replace ? <Invoice>[] : await load();
    final existingIds = existing.map((e) => e.id).toSet();
    int added = 0;
    for (final inv in imported) {
      if (!existingIds.contains(inv.id)) { existing.add(inv); added++; }
    }
    await saveAll(existing);
    return added;
  }
}

/* ======================== أدوات ======================== */

final NumberFormat _currency = NumberFormat("#,##0.00", AppConfig.numberLocale);
final DateFormat _dateFormatter = DateFormat(AppConfig.dateFormat, "ar");

String formatCurrency(double v) {
  try { 
    return '${_currency.format(v)} ${AppConfig.currencySymbol}'; 
  } catch (_) { 
    return '${v.toStringAsFixed(2)} ${AppConfig.currencySymbol}'; 
  }
}

// ✅ الإصلاح: دالة formatDate صحيحة نحوياً وبدون أخطاء
String formatDate(DateTime dt) {
  try {
    return _dateFormatter.format(dt);
  } catch (_) {
    final year = dt.year.toString();
    final month = dt.month.toString().padLeft(2, '0');
    final day = dt.day.toString().padLeft(2, '0');
    return '$year/$month/$day';
  }
}

String generateInvoiceNumber() {
  final ts = DateTime.now().millisecondsSinceEpoch.toString();
  return ts.length > 5 ? ts.substring(5) : ts;
}

/* ========================= الشاشة الرئيسية ========================= */

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(AppConfig.appTitle),
        actions: [
          IconButton(
            tooltip: 'الإعدادات',
            onPressed: () => Navigator.push(context,
                MaterialPageRoute(builder: (_) => const SettingsScreen())),
            icon: const Icon(Icons.settings_outlined),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            const SizedBox(height: 20),
            _card(context, icon: Icons.flash_on, color: Colors.orange,
              title: 'الوضع السريع', sub: 'حساب سريع وإيصال PDF فوري',
              onTap: () => Navigator.push(context,
                  MaterialPageRoute(builder: (_) => const QuickCalcScreen()))),
            const SizedBox(height: 16),
            _card(context, icon: Icons.receipt_long, color: t.colorScheme.primary,
              title: 'فاتورة كاملة', sub: 'فاتورة احترافية بكل التفاصيل',
              onTap: () => Navigator.push(context,
                  MaterialPageRoute(builder: (_) => const InvoiceScreen()))),
            const SizedBox(height: 16),
            _card(context, icon: Icons.history, color: Colors.blue,
              title: 'الفواتير المحفوظة', sub: 'عرض، بحث، وإدارة الفواتير',
              onTap: () => Navigator.push(context,
                  MaterialPageRoute(builder: (_) => const SavedInvoicesScreen()))),
            const SizedBox(height: 16),
            _card(context, icon: Icons.inventory_2_outlined, color: Colors.green,
              title: 'دليل السلع والمنتجات', sub: 'إضافة وتعديل وحذف والبحث في السلع',
              onTap: () => Navigator.push(context,
                  MaterialPageRoute(builder: (_) => const ProductsScreen()))),
            const Spacer(),
            Text('v2.0 - دليل السلع مفعّل', style: TextStyle(color: t.colorScheme.outline, fontSize: 12)),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  Widget _card(BuildContext context, {required IconData icon, required Color color,
      required String title, required String sub, required VoidCallback onTap}) {
    final t = Theme.of(context);
    return Card(
      elevation: 3,
      child: InkWell(
        onTap: onTap, borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Row(
            children: [
              Container(width: 60, height: 60,
                decoration: BoxDecoration(color: color.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(16)),
                child: Icon(icon, color: color, size: 32)),
              const SizedBox(width: 16),
              Expanded(child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 4),
                  Text(sub, style: TextStyle(color: t.colorScheme.outline, fontSize: 13)),
                ],
              )),
              Icon(Icons.arrow_forward_ios, size: 16, color: t.colorScheme.outline),
            ],
          ),
        ),
      ),
    );
  }
}

/* ========================= شاشة الفاتورة الكاملة ========================= */

class InvoiceScreen extends StatefulWidget {
  final List<InvoiceItem>? initialItems;
  const InvoiceScreen({super.key, this.initialItems});
  @override
  State<InvoiceScreen> createState() => _InvoiceScreenState();
}

class _InvoiceScreenState extends State<InvoiceScreen> {
  final _customerCtrl = TextEditingController();
  final _invoiceNumberCtrl = TextEditingController();
  final _discountCtrl = TextEditingController(text: '0');
  final _taxCtrl = TextEditingController(text: '0');
  final List<InvoiceItem> _items = [];
  List<QuickItem> _products = [];
  String _status = 'unpaid';
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _invoiceNumberCtrl.text = generateInvoiceNumber();
    if (widget.initialItems != null) _items.addAll(widget.initialItems!);
    _loadProducts();
    _discountCtrl.addListener(_refresh);
    _taxCtrl.addListener(_refresh);
  }

  @override
  void dispose() {
    _discountCtrl.removeListener(_refresh);
    _taxCtrl.removeListener(_refresh);
    _customerCtrl.dispose();
    _invoiceNumberCtrl.dispose();
    _discountCtrl.dispose();
    _taxCtrl.dispose();
    super.dispose();
  }

  void _refresh() { if (mounted) setState(() {}); }

  Future<void> _loadProducts() async {
    final l = await ProductStore.load();
    if (mounted) setState(() => _products = l);
  }

  void _addProduct(QuickItem p) {
    setState(() {
      final i = _items.indexWhere((e) => e.name == p.name && e.price == p.price);
      if (i >= 0) {
        final o = _items[i];
        _items[i] = InvoiceItem(name: o.name, qty: o.qty + 1, price: o.price);
      } else {
        _items.add(InvoiceItem(name: p.name, qty: 1, price: p.price));
      }
    });
  }

  void _deleteProduct(QuickItem p) {
    showDialog(context: context, builder: (ctx) => AlertDialog(
      title: const Text('حذف من الأصناف المحفوظة'), content: Text(p.name),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () async {
          Navigator.pop(ctx);
          await ProductStore.remove(p.name);
          await _loadProducts();
        }, child: const Text('حذف')),
      ],
    ));
  }

  double get _subtotal => _items.fold(0.0, (s, i) => s + i.total);
  double get _discount => double.tryParse(_discountCtrl.text.trim()) ?? 0;
  double get _taxRate => double.tryParse(_taxCtrl.text.trim()) ?? 0;
  double get _taxAmount => _subtotal * (_taxRate / 100);
  double get _grandTotal => _subtotal - _discount + _taxAmount;
  String get _today => formatDate(DateTime.now());

  void _snack(String m) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(m), behavior: SnackBarBehavior.floating));
  }

  Future<InvoiceItem?> _itemDialog({InvoiceItem? initial}) async {
    final n = TextEditingController(text: initial?.name ?? '');
    final q = TextEditingController(text: initial?.qty.toString() ?? '1');
    final p = TextEditingController(text: initial?.price.toString() ?? '');
    final fk = GlobalKey<FormState>();
    bool saveProduct = true;
    final nf = FocusNode();
    final qf = FocusNode();

    final res = await showDialog<InvoiceItem>(
      context: context,
      builder: (ctx) => StatefulBuilder(builder: (ctx, setD) => AlertDialog(
        title: Text(initial == null ? 'إضافة صنف' : 'تعديل صنف'),
        content: Form(key: fk, child: SingleChildScrollView(child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            RawAutocomplete<QuickItem>(
              textEditingController: n,
              focusNode: nf,
              displayStringForOption: (o) => o.name,
              optionsBuilder: (v) {
                final q = v.text.trim();
                if (q.isEmpty) return _products;
                return _products.where((e) => e.name.contains(q));
              },
              onSelected: (o) { p.text = o.price.toString(); qf.requestFocus(); },
              fieldViewBuilder: (c, ctrl, focus, onSubmit) => TextFormField(
                controller: ctrl, focusNode: focus,
                decoration: const InputDecoration(labelText: 'اسم الصنف',
                    hintText: 'اكتب أو اختر من المحفوظة'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'أدخل الاسم' : null),
              optionsViewBuilder: (c, onSel, opts) => Align(
                alignment: AlignmentDirectional.topStart,
                child: Material(elevation: 4, child: ConstrainedBox(
                  constraints: const BoxConstraints(maxHeight: 220, maxWidth: 260),
                  child: ListView(padding: EdgeInsets.zero, shrinkWrap: true,
                    children: opts.map((o) => ListTile(dense: true,
                      title: Text(o.name), trailing: Text(formatCurrency(o.price)),
                      onTap: () => onSel(o))).toList()),
                )),
              ),
            ),
            const SizedBox(height: 12),
            TextFormField(controller: q, focusNode: qf,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'الكمية'),
              validator: (v) {
                final x = double.tryParse(v?.trim() ?? '');
                return (x == null || x <= 0) ? 'كمية غير صحيحة' : null;
              }),
            const SizedBox(height: 12),
            TextFormField(controller: p,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'السعر'),
              validator: (v) {
                final x = double.tryParse(v?.trim() ?? '');
                return (x == null || x < 0) ? 'سعر غير صحيح' : null;
              }),
            CheckboxListTile(contentPadding: EdgeInsets.zero,
              value: saveProduct,
              onChanged: (v) => setD(() => saveProduct = v ?? false),
              title: const Text('حفظ الاسم والسعر للفواتير القادمة')),
          ],
        ))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('إلغاء')),
          ElevatedButton(
            onPressed: () {
              if (fk.currentState!.validate()) {
                if (saveProduct) {
                  ProductStore.upsert(QuickItem(name: n.text.trim(),
                      price: double.parse(p.text.trim()))).then((_) => _loadProducts());
                }
                Navigator.pop(ctx, InvoiceItem(
                  name: n.text.trim(),
                  qty: double.parse(q.text.trim()),
                  price: double.parse(p.text.trim()),
                ));
              }
            }, child: const Text('حفظ')),
        ],
      )),
    );
    WidgetsBinding.instance.addPostFrameCallback((_) {
      n.dispose(); q.dispose(); p.dispose(); nf.dispose(); qf.dispose();
    });
    return res;
  }

  Future<void> _addItem() async {
    final item = await _itemDialog();
    if (item != null && mounted) setState(() => _items.add(item));
  }

  Future<void> _editItem(int i) async {
    if (i < 0 || i >= _items.length) return;
    final u = await _itemDialog(initial: _items[i]);
    if (u != null && mounted) setState(() => _items[i] = u);
  }

  void _removeItem(int i) {
    showDialog(context: context, builder: (ctx) => AlertDialog(
      title: const Text('حذف الصنف'), content: const Text('هل أنت متأكد؟'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () { Navigator.pop(ctx); if (mounted) setState(() => _items.removeAt(i)); },
          child: const Text('حذف')),
      ],
    ));
  }

  Invoice _current() => Invoice(
    id: DateTime.now().microsecondsSinceEpoch.toString(),
    invoiceNumber: _invoiceNumberCtrl.text.trim().isEmpty
        ? generateInvoiceNumber() : _invoiceNumberCtrl.text.trim(),
    customer: _customerCtrl.text.trim().isEmpty
        ? 'عميل نقدي' : _customerCtrl.text.trim(),
    date: _today, items: List.of(_items),
    discount: _discount, taxRate: _taxRate, status: _status,
  );

  Future<void> _save() async {
    if (_items.isEmpty) { _snack('أضف صنفاً واحداً على الأقل'); return; }
    try { await InvoiceStore.add(_current()); _snack('تم حفظ الفاتورة'); }
    catch (e) { _snack('خطأ: $e'); }
  }

  Future<void> _share() async {
    if (_items.isEmpty) { _snack('أضف صنفاً واحداً على الأقل'); return; }
    setState(() => _busy = true);
    try {
      final inv = _current();
      final doc = await buildFullPdf(inv);
      await Printing.sharePdf(bytes: await doc.save(),
          filename: 'invoice_${inv.invoiceNumber}.pdf');
    } catch (e) { _snack('تعذر: $e'); }
    finally { if (mounted) setState(() => _busy = false); }
  }

  Future<void> _print() async {
    if (_items.isEmpty) { _snack('أضف صنفاً واحداً على الأقل'); return; }
    setState(() => _busy = true);
    try {
      final inv = _current();
      final doc = await buildFullPdf(inv);
      await Printing.layoutPdf(onLayout: (_) => doc.save(),
          name: 'invoice_${inv.invoiceNumber}.pdf');
    } catch (e) { _snack('تعذر: $e'); }
    finally { if (mounted) setState(() => _busy = false); }
  }

  void _new() {
    setState(() {
      _customerCtrl.clear();
      _invoiceNumberCtrl.text = generateInvoiceNumber();
      _discountCtrl.text = '0';
      _taxCtrl.text = '0';
      _status = 'unpaid';
      _items.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('فاتورة كاملة'),
        actions: [
          IconButton(tooltip: 'جديدة', onPressed: _new, icon: const Icon(Icons.note_add_outlined)),
          IconButton(tooltip: 'المحفوظة',
            onPressed: () => Navigator.push(context,
                MaterialPageRoute(builder: (_) => const SavedInvoicesScreen())),
            icon: const Icon(Icons.history)),
        ],
      ),
      body: Column(
        children: [
          Expanded(child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Card(child: Padding(padding: const EdgeInsets.all(12), child: Column(
                children: [
                  TextField(controller: _customerCtrl,
                    decoration: const InputDecoration(labelText: 'اسم العميل',
                        prefixIcon: Icon(Icons.person_outline))),
                  const SizedBox(height: 12),
                  Row(children: [
                    Expanded(child: TextField(controller: _invoiceNumberCtrl,
                        decoration: const InputDecoration(labelText: 'رقم الفاتورة'))),
                    const SizedBox(width: 12),
                    Expanded(child: InputDecorator(
                        decoration: const InputDecoration(labelText: 'التاريخ'),
                        child: Text(_today))),
                  ]),
                  const SizedBox(height: 12),
                  Row(children: [
                    Expanded(child: TextField(controller: _discountCtrl,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(labelText: 'الخصم'))),
                    const SizedBox(width: 12),
                    Expanded(child: TextField(controller: _taxCtrl,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(labelText: 'الضريبة %'))),
                  ]),
                  const SizedBox(height: 12),
                  Row(children: [
                    const Text('حالة الدفع:'), const SizedBox(width: 12),
                    Expanded(child: SegmentedButton<String>(
                      segments: const [
                        ButtonSegment(value: 'unpaid', label: Text('غير مدفوعة'), icon: Icon(Icons.schedule)),
                        ButtonSegment(value: 'paid', label: Text('مدفوعة'), icon: Icon(Icons.check_circle)),
                      ],
                      selected: {_status},
                      onSelectionChanged: (s) => setState(() => _status = s.first),
                    )),
                  ]),
                ],
              ))),
              const SizedBox(height: 16),
              Row(children: [
                Text('الأصناف', style: t.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                const Spacer(),
                Text('${_items.length} صنف'),
              ]),
              const Divider(),
              if (_products.isNotEmpty) ...[
                Text('أصنافك المحفوظة (اضغط للإضافة، اضغط مطولاً للحذف)',
                    style: TextStyle(fontSize: 12, color: t.colorScheme.outline)),
                const SizedBox(height: 6),
                Wrap(spacing: 6, runSpacing: 6, children: _products.map((p) =>
                  GestureDetector(onLongPress: () => _deleteProduct(p),
                    child: ActionChip(
                      avatar: const Icon(Icons.add, size: 16),
                      label: Text('${p.name} • ${formatCurrency(p.price)}'),
                      onPressed: () => _addProduct(p)))).toList()),
                const Divider(),
              ],
              if (_items.isEmpty)
                Padding(padding: const EdgeInsets.symmetric(vertical: 40),
                  child: Center(child: Column(children: [
                    Icon(Icons.receipt_long_outlined, size: 56, color: t.colorScheme.outline),
                    const SizedBox(height: 12),
                    const Text('لا توجد أصناف بعد'),
                  ])))
              else
                ..._items.asMap().entries.map((e) => Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    leading: CircleAvatar(backgroundColor: t.colorScheme.primaryContainer,
                        child: Text('${e.key + 1}')),
                    title: Text(e.value.name),
                    subtitle: Text('${e.value.qty.toStringAsFixed(e.value.qty % 1 == 0 ? 0 : 2)} × ${formatCurrency(e.value.price)}'),
                    trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                      Text(formatCurrency(e.value.total), style: TextStyle(
                          fontWeight: FontWeight.bold, color: t.colorScheme.primary)),
                      IconButton(icon: const Icon(Icons.edit_outlined), onPressed: () => _editItem(e.key)),
                      IconButton(icon: const Icon(Icons.delete_outline), onPressed: () => _removeItem(e.key)),
                    ]),
                  ),
                )),
            ],
          )),
          _bottomBar(t),
        ],
      ),
    );
  }

  Widget _bottomBar(ThemeData t) => Material(
    elevation: 8, color: t.colorScheme.surface,
    child: SafeArea(top: false, child: Padding(
      padding: const EdgeInsets.all(12),
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        _row('المجموع الفرعي', _subtotal),
        if (_discount > 0) _row('الخصم', -_discount),
        if (_taxRate > 0) _row('الضريبة (${_taxRate.toStringAsFixed(1)}%)', _taxAmount),
        const Divider(),
        Row(children: [
          const Text('الإجمالي:', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const Spacer(),
          Text(formatCurrency(_grandTotal), style: TextStyle(
              fontSize: 24, fontWeight: FontWeight.bold, color: t.colorScheme.primary)),
        ]),
        const SizedBox(height: 12),
        Row(children: [
          Expanded(child: OutlinedButton.icon(onPressed: _busy ? null : _addItem,
              icon: const Icon(Icons.add), label: const Text('إضافة'))),
          const SizedBox(width: 8),
          Expanded(child: ElevatedButton.icon(onPressed: _busy ? null : _save,
              icon: const Icon(Icons.save_outlined), label: const Text('حفظ'))),
          const SizedBox(width: 8),
          Expanded(child: ElevatedButton.icon(onPressed: _busy ? null : _print,
              icon: _busy ? const SizedBox(width: 16, height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2))
                  : const Icon(Icons.print_outlined),
              label: const Text('طباعة'))),
        ]),
        const SizedBox(height: 8),
        SizedBox(width: double.infinity, child: OutlinedButton.icon(
            onPressed: _busy ? null : _share,
            icon: const Icon(Icons.share_outlined), label: const Text('مشاركة PDF'))),
      ]),
    )),
  );

  Widget _row(String l, double v) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 2),
    child: Row(children: [
      Text(l), const Spacer(),
      Text(formatCurrency(v), style: const TextStyle(fontWeight: FontWeight.w500)),
    ]),
  );
}

/* ====================== الوضع السريع ====================== */

class QuickCalcScreen extends StatefulWidget {
  const QuickCalcScreen({super.key});
  @override
  State<QuickCalcScreen> createState() => _QuickCalcScreenState();
}

class _QuickCalcScreenState extends State<QuickCalcScreen> {
  final _nameCtrl = TextEditingController();
  final _priceCtrl = TextEditingController();
  final _qtyCtrl = TextEditingController(text: '1');
  final _notesCtrl = TextEditingController();
  final _priceFocus = FocusNode();

  final List<InvoiceItem> _items = [];
  List<QuickItem> _favorites = [];
  List<QuickItem> _products = [];
  final _qtyFocus = FocusNode();
  final _nameFocus = FocusNode();
  bool _busy = false;
  bool _addToSaved = false;
  bool _showKeypad = false;
  double _discountPercent = 0;
  double _taxPercent = 0;
  String _calcExpression = '';
  String _calcResult = '0';

  @override
  void initState() { super.initState(); _loadFavorites(); _loadProducts(); }

  @override
  void dispose() {
    _nameCtrl.dispose(); _priceCtrl.dispose(); _qtyCtrl.dispose();
    _notesCtrl.dispose(); _priceFocus.dispose(); _qtyFocus.dispose(); _nameFocus.dispose();
    super.dispose();
  }

  Future<void> _loadFavorites() async {
    final list = await QuickItemsStore.load();
    if (mounted) setState(() => _favorites = list);
  }

  Future<void> _loadProducts() async {
    final list = await ProductStore.load();
    if (mounted) setState(() => _products = list);
  }

  double get _subtotal => _items.fold(0.0, (s, i) => s + i.total);
  double get _discountAmount => _subtotal * (_discountPercent / 100);
  double get _taxAmount => (_subtotal - _discountAmount) * (_taxPercent / 100);
  double get _total => _subtotal - _discountAmount + _taxAmount;

  void _snack(String m) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(m), behavior: SnackBarBehavior.floating,
          duration: const Duration(seconds: 1)));
  }

  void _add() {
    final price = double.tryParse(_priceCtrl.text.trim());
    if (price == null || price <= 0) { _snack('أدخل سعراً صحيحاً'); return; }
    final qty = double.tryParse(_qtyCtrl.text.trim()) ?? 1;
    if (qty <= 0) { _snack('كمية غير صحيحة'); return; }
    setState(() {
      _items.add(InvoiceItem(
        name: _nameCtrl.text.trim().isEmpty ? 'صنف ${_items.length + 1}' : _nameCtrl.text.trim(),
        qty: qty, price: price,
      ));
      _nameCtrl.clear(); _priceCtrl.clear(); _qtyCtrl.text = '1';
    });
    _priceFocus.requestFocus();
  }

  void _addFromFavorite(QuickItem fav) {
    setState(() => _items.add(InvoiceItem(name: fav.name, qty: 1, price: fav.price)));
    _snack('+ ${fav.name}');
  }

  Future<void> _saveAsFavorite() async {
    final name = _nameCtrl.text.trim();
    final price = double.tryParse(_priceCtrl.text.trim()) ?? 0;
    if (name.isEmpty || price <= 0) { _snack('اكتب الاسم والسعر أولاً'); return; }
    await QuickItemsStore.add(QuickItem(name: name, price: price));
    await _loadFavorites();
    _snack('تم الحفظ في الشائعة');
  }

  Future<void> _deleteFavorite(QuickItem fav) async {
    await QuickItemsStore.remove(fav.name);
    await _loadFavorites();
  }

  void _removeLast() {
    if (_items.isEmpty) return;
    setState(() => _items.removeLast());
  }

  Future<void> _editItem(int i) async {
    final n = TextEditingController(text: _items[i].name);
    final q = TextEditingController(text: _items[i].qty.toString());
    final p = TextEditingController(text: _items[i].price.toString());
    final ok = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('تعديل صنف'),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        TextField(controller: n, decoration: const InputDecoration(labelText: 'الاسم')),
        const SizedBox(height: 8),
        TextField(controller: q, keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(labelText: 'الكمية')),
        const SizedBox(height: 8),
        TextField(controller: p, keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(labelText: 'السعر')),
      ]),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('حفظ')),
      ],
    ));
    if (ok == true) {
      final nq = double.tryParse(q.text.trim()) ?? 1;
      final np = double.tryParse(p.text.trim()) ?? 0;
      if (nq > 0 && np >= 0) {
        setState(() => _items[i] = InvoiceItem(
          name: n.text.trim().isEmpty ? _items[i].name : n.text.trim(),
          qty: nq, price: np));
      }
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      n.dispose(); q.dispose(); p.dispose();
    });
  }

  void _clearAll() {
    if (_items.isEmpty) return;
    showDialog(context: context, builder: (ctx) => AlertDialog(
      title: const Text('مسح الكل'), content: const Text('سيتم حذف كل الأصناف. متأكد؟'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () {
          setState(() { _items.clear(); _notesCtrl.clear();
            _discountPercent = 0; _taxPercent = 0; });
          Navigator.pop(ctx);
        }, child: const Text('مسح')),
      ],
    ));
  }

  void _calcKey(String key) {
    setState(() {
      if (key == 'C') { _calcExpression = ''; _calcResult = '0'; }
      else if (key == '←') {
        if (_calcExpression.isNotEmpty)
          _calcExpression = _calcExpression.substring(0, _calcExpression.length - 1);
        _calcResult = _calcExpression.isEmpty ? '0' : _calcExpression;
      } else if (key == '=') {
        try { final r = _evalExpression(_calcExpression); _calcResult = r; _calcExpression = r; }
        catch (_) { _calcResult = 'خطأ'; }
      } else if (key == '→') {
        final v = double.tryParse(_calcResult);
        if (v != null && v > 0) {
          _priceCtrl.text = v.toStringAsFixed(2);
          _showKeypad = false;
          _priceFocus.requestFocus();
        }
      } else { _calcExpression += key; _calcResult = _calcExpression; }
    });
  }

  String _evalExpression(String expr) {
    if (expr.trim().isEmpty) return '0';
    expr = expr.replaceAll('×', '*').replaceAll('÷', '/');
    try {
      final r = _parseAndEval(expr);
      if (r.isNaN || r.isInfinite) return 'خطأ';
      if (r % 1 == 0) return r.toInt().toString();
      return r.toStringAsFixed(2);
    } catch (_) { return 'خطأ'; }
  }

  double _parseAndEval(String expr) {
    expr = expr.replaceAll(' ', '');
    final parts = <String>[]; final ops = <String>[]; var current = '';
    for (var i = 0; i < expr.length; i++) {
      final c = expr[i];
      if ('+-*/'.contains(c) && i > 0) { parts.add(current); ops.add(c); current = ''; }
      else { current += c; }
    }
    parts.add(current);
    final nums = parts.map(double.parse).toList();
    var i = 0;
    while (i < ops.length) {
      if (ops[i] == '*') { nums[i] = nums[i] * nums[i + 1]; nums.removeAt(i + 1); ops.removeAt(i); }
      else if (ops[i] == '/') { nums[i] = nums[i] / nums[i + 1]; nums.removeAt(i + 1); ops.removeAt(i); }
      else { i++; }
    }
    var r = nums[0];
    for (var j = 0; j < ops.length; j++) {
      if (ops[j] == '+') r += nums[j + 1];
      if (ops[j] == '-') r -= nums[j + 1];
    }
    return r;
  }

  Invoice _current() => Invoice(
    id: DateTime.now().microsecondsSinceEpoch.toString(),
    invoiceNumber: generateInvoiceNumber(),
    customer: 'عميل نقدي', date: formatDate(DateTime.now()),
    items: List.of(_items), discount: _discountAmount, status: 'paid',
  );

  Future<void> _sharePdf() async {
    if (_items.isEmpty) { _snack('أضف صنفاً واحداً على الأقل'); return; }
    setState(() => _busy = true);
    try {
      final inv = _current();
      final doc = await buildQuickPdf(inv, notes: _notesCtrl.text.trim(), taxAmount: _taxAmount);
      await Printing.sharePdf(bytes: await doc.save(),
          filename: 'invoice_${inv.invoiceNumber}.pdf');
      if (_addToSaved) await InvoiceStore.add(inv);
    } catch (e) { _snack('تعذر: $e'); }
    finally { if (mounted) setState(() => _busy = false); }
  }

  Future<void> _printPdf() async {
    if (_items.isEmpty) { _snack('أضف صنفاً واحداً على الأقل'); return; }
    setState(() => _busy = true);
    try {
      final inv = _current();
      final doc = await buildQuickPdf(inv, notes: _notesCtrl.text.trim(), taxAmount: _taxAmount);
      await Printing.layoutPdf(onLayout: (_) => doc.save(),
          name: 'invoice_${inv.invoiceNumber}.pdf');
    } catch (e) { _snack('تعذر: $e'); }
    finally { if (mounted) setState(() => _busy = false); }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('الوضع السريع'),
        actions: [
          IconButton(tooltip: _showKeypad ? 'إخفاء' : 'الآلة',
            onPressed: () => setState(() => _showKeypad = !_showKeypad),
            icon: Icon(_showKeypad ? Icons.keyboard_hide_outlined : Icons.calculate_outlined),
            color: _showKeypad ? t.colorScheme.primary : null),
          IconButton(tooltip: 'نقل إلى فاتورة كاملة',
            onPressed: _items.isEmpty ? null : () => Navigator.push(context,
                MaterialPageRoute(builder: (_) => InvoiceScreen(
                    initialItems: _items.map((e) => e.copy()).toList()))),
            icon: const Icon(Icons.receipt_long_outlined)),
          IconButton(tooltip: 'مسح الكل',
            onPressed: _items.isEmpty ? null : _clearAll,
            icon: const Icon(Icons.delete_sweep_outlined)),
        ],
      ),
      body: Column(children: [
        _buildTotalDisplay(t),
        if (_showKeypad) _buildCalculator(t),
        if (_favorites.isNotEmpty && !_showKeypad) _buildFavorites(t),
        if (!_showKeypad) _buildInputRow(t),
        Expanded(child: _buildItemsList(t)),
        if (_items.isNotEmpty) _buildQuickPercents(t),
        _buildBottomBar(t),
      ]),
    );
  }

  Widget _buildTotalDisplay(ThemeData t) => Container(
    width: double.infinity, padding: const EdgeInsets.symmetric(vertical: 16),
    color: t.colorScheme.primaryContainer,
    child: Column(children: [
      Text('الإجمالي', style: TextStyle(
          color: t.colorScheme.onPrimaryContainer.withOpacity(0.7), fontSize: 14)),
      const SizedBox(height: 4),
      Text(formatCurrency(_total), style: TextStyle(fontSize: 42,
          fontWeight: FontWeight.bold, color: t.colorScheme.onPrimaryContainer)),
      if (_discountPercent > 0 || _taxPercent > 0)
        Padding(padding: const EdgeInsets.only(top: 4),
          child: Wrap(spacing: 8, alignment: WrapAlignment.center, children: [
            if (_discountPercent > 0) Chip(
              label: Text('خصم ${_discountPercent.toStringAsFixed(0)}%'),
              backgroundColor: Colors.red.shade100,
              labelStyle: TextStyle(color: Colors.red.shade900),
              visualDensity: VisualDensity.compact),
            if (_taxPercent > 0) Chip(
              label: Text('ضريبة ${_taxPercent.toStringAsFixed(0)}%'),
              backgroundColor: Colors.blue.shade100,
              labelStyle: TextStyle(color: Colors.blue.shade900),
              visualDensity: VisualDensity.compact),
          ])),
      Text('${_items.length} صنف', style: TextStyle(
          color: t.colorScheme.onPrimaryContainer.withOpacity(0.6), fontSize: 12)),
    ]),
  );

  Widget _buildCalculator(ThemeData t) {
    final keys = [
      ['C', '←', '÷', '×'], ['7', '8', '9', '-'], ['4', '5', '6', '+'],
      ['1', '2', '3', '='], ['0', '.', '→', '='],
    ];
    return Container(padding: const EdgeInsets.all(8),
      color: t.colorScheme.surfaceContainerHighest,
      child: Column(children: [
        Container(width: double.infinity, padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(color: t.colorScheme.surface,
              borderRadius: BorderRadius.circular(8)),
          child: Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Text(_calcExpression.isEmpty ? '0' : _calcExpression,
              style: TextStyle(fontSize: 16, color: t.colorScheme.outline),
              maxLines: 1, overflow: TextOverflow.ellipsis),
            const SizedBox(height: 4),
            Text(_calcResult, style: TextStyle(fontSize: 28,
                fontWeight: FontWeight.bold, color: t.colorScheme.primary)),
          ])),
        const SizedBox(height: 8),
        ...keys.map((row) => Padding(padding: const EdgeInsets.only(bottom: 6),
          child: Row(children: row.asMap().entries.map((entry) {
            final idx = entry.key; final key = entry.value;
            if (key == '=' && idx > 0 && row[idx - 1] == '=') return const Expanded(child: SizedBox());
            final isOp = '+-×÷=→'.contains(key);
            final isAct = 'C←'.contains(key);
            return Expanded(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 3),
              child: SizedBox(height: 48, child: ElevatedButton(
                onPressed: () => _calcKey(key),
                style: ElevatedButton.styleFrom(
                  backgroundColor: isAct ? Colors.red.shade100
                      : isOp ? t.colorScheme.primaryContainer : t.colorScheme.surface,
                  foregroundColor: isAct ? Colors.red.shade900 : t.colorScheme.onSurface,
                  padding: EdgeInsets.zero),
                child: Text(key, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              ))));
          }).toList()))),
      ]));
  }

  // ✅ الإصلاح: لف ActionChip داخل GestureDetector لدعم onLongPress
  Widget _buildFavorites(ThemeData t) => Container(
    height: 70, padding: const EdgeInsets.symmetric(vertical: 6),
    child: ListView.builder(scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      itemCount: _favorites.length, itemBuilder: (ctx, i) {
        final fav = _favorites[i];
        return Padding(padding: const EdgeInsets.symmetric(horizontal: 4),
          child: GestureDetector(
            onLongPress: () => _deleteFavorite(fav),
            child: ActionChip(
              avatar: CircleAvatar(backgroundColor: t.colorScheme.primary,
                  child: Text(fav.name.substring(0, 1),
                      style: const TextStyle(color: Colors.white, fontSize: 12))),
              label: Column(mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(fav.name, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                  Text(formatCurrency(fav.price), style: TextStyle(
                      fontSize: 10, color: t.colorScheme.outline)),
                ]),
              onPressed: () => _addFromFavorite(fav),
            ),
          ));
      }),
  );

  Widget _buildInputRow(ThemeData t) => Padding(
    padding: const EdgeInsets.all(12),
    child: Column(children: [
      Row(children: [
        Expanded(flex: 2, child: RawAutocomplete<QuickItem>(
          textEditingController: _nameCtrl,
          focusNode: _nameFocus,
          displayStringForOption: (o) => o.name,
          optionsBuilder: (v) {
            final q = v.text.trim();
            if (q.isEmpty) return const <QuickItem>[];
            final starts = _products.where((e) => e.name.startsWith(q));
            final contains = _products.where((e) => !e.name.startsWith(q) && e.name.contains(q));
            return [...starts, ...contains];
          },
          onSelected: (o) {
            _priceCtrl.text = o.price.toString();
            _qtyFocus.requestFocus();
          },
          fieldViewBuilder: (c, ctrl, focus, onSubmit) => TextField(
            controller: ctrl, focusNode: focus,
            textInputAction: TextInputAction.next,
            decoration: const InputDecoration(labelText: 'الاسم (اختياري)',
                prefixIcon: Icon(Icons.label_outline))),
          optionsViewBuilder: (c, onSel, opts) => Align(
            alignment: AlignmentDirectional.topStart,
            child: Material(elevation: 4, child: ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 220, maxWidth: 260),
              child: ListView(padding: EdgeInsets.zero, shrinkWrap: true,
                children: opts.map((o) => ListTile(dense: true,
                  title: Text(o.name), trailing: Text(formatCurrency(o.price)),
                  onTap: () => onSel(o))).toList()),
            )),
          ),
        )),
        const SizedBox(width: 8),
        Expanded(flex: 1, child: TextField(controller: _qtyCtrl, focusNode: _qtyFocus,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          textInputAction: TextInputAction.next,
          decoration: const InputDecoration(labelText: 'الكمية'))),
      ]),
      const SizedBox(height: 8),
      Row(children: [
        Expanded(child: TextField(controller: _priceCtrl, focusNode: _priceFocus,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          textInputAction: TextInputAction.done, onSubmitted: (_) => _add(),
          decoration: const InputDecoration(labelText: 'السعر',
              prefixIcon: Icon(Icons.attach_money)))),
        IconButton(tooltip: 'حفظ في الشائعة',
          onPressed: _saveAsFavorite,
          icon: const Icon(Icons.bookmark_add_outlined)),
        SizedBox(height: 48, child: ElevatedButton.icon(
          onPressed: _add, icon: const Icon(Icons.add), label: const Text('إضافة'),
          style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 16)))),
      ]),
    ]),
  );

  Widget _buildItemsList(ThemeData t) {
    if (_items.isEmpty) {
      return Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
        Icon(Icons.flash_on, size: 64, color: t.colorScheme.outline),
        const SizedBox(height: 12),
        Text('ابدأ بإضافة الأسعار', style: TextStyle(color: t.colorScheme.outline, fontSize: 16)),
        if (_favorites.isNotEmpty)
          Padding(padding: const EdgeInsets.only(top: 12),
            child: Text('أو اضغط على صنف من الشائعة ↑',
                style: TextStyle(color: t.colorScheme.outline, fontSize: 12))),
      ]));
    }
    return ListView.builder(padding: const EdgeInsets.symmetric(horizontal: 12),
      itemCount: _items.length, itemBuilder: (ctx, i) {
        final realIndex = _items.length - 1 - i;
        final item = _items[realIndex];
        return Card(margin: const EdgeInsets.only(bottom: 6),
          child: ListTile(dense: true,
            leading: CircleAvatar(radius: 16,
              backgroundColor: t.colorScheme.primaryContainer,
              child: Text('${realIndex + 1}', style: const TextStyle(fontSize: 12))),
            title: Text(item.name),
            subtitle: Text('${item.qty.toStringAsFixed(item.qty % 1 == 0 ? 0 : 2)} × ${formatCurrency(item.price)}'),
            trailing: Row(mainAxisSize: MainAxisSize.min, children: [
              Text(formatCurrency(item.total), style: TextStyle(
                  fontWeight: FontWeight.bold, color: t.colorScheme.primary, fontSize: 16)),
              IconButton(icon: const Icon(Icons.edit_outlined, size: 20),
                  onPressed: () => _editItem(realIndex)),
            ]),
          ));
      });
  }

  Widget _buildQuickPercents(ThemeData t) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
    color: t.colorScheme.surfaceContainerHighest,
    child: SingleChildScrollView(scrollDirection: Axis.horizontal,
      child: Row(children: [
        const Text('خصم:', style: TextStyle(fontSize: 12)), const SizedBox(width: 4),
        ...[0, 5, 10, 15, 20].map((p) => Padding(
          padding: const EdgeInsets.symmetric(horizontal: 2),
          child: ChoiceChip(label: Text('$p%', style: const TextStyle(fontSize: 11)),
            selected: _discountPercent == p,
            onSelected: (_) => setState(() => _discountPercent = p.toDouble()),
            visualDensity: VisualDensity.compact,
            labelPadding: const EdgeInsets.symmetric(horizontal: 4)))),
        const SizedBox(width: 16),
        const Text('ضريبة:', style: TextStyle(fontSize: 12)), const SizedBox(width: 4),
        ...[0, 5, 10, 15].map((p) => Padding(
          padding: const EdgeInsets.symmetric(horizontal: 2),
          child: ChoiceChip(label: Text('$p%', style: const TextStyle(fontSize: 11)),
            selected: _taxPercent == p,
            onSelected: (_) => setState(() => _taxPercent = p.toDouble()),
            visualDensity: VisualDensity.compact,
            labelPadding: const EdgeInsets.symmetric(horizontal: 4)))),
      ])),
  );

  Widget _buildBottomBar(ThemeData t) => Material(
    elevation: 8, color: t.colorScheme.surface,
    child: SafeArea(top: false, child: Padding(
      padding: const EdgeInsets.all(12),
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        TextField(controller: _notesCtrl, maxLines: 1,
          decoration: const InputDecoration(labelText: 'ملاحظات',
              prefixIcon: Icon(Icons.notes), isDense: true)),
        CheckboxListTile(dense: true, contentPadding: EdgeInsets.zero,
          value: _addToSaved,
          onChanged: (v) => setState(() => _addToSaved = v ?? false),
          title: const Text('حفظ في سجل الفواتير', style: TextStyle(fontSize: 13)),
          controlAffinity: ListTileControlAffinity.leading),
        Row(children: [
          Expanded(child: OutlinedButton.icon(onPressed: _items.isEmpty ? null : _removeLast,
              icon: const Icon(Icons.undo), label: const Text('تراجع'))),
          const SizedBox(width: 8),
          Expanded(child: OutlinedButton.icon(onPressed: _busy ? null : _sharePdf,
              icon: const Icon(Icons.share_outlined), label: const Text('مشاركة'))),
          const SizedBox(width: 8),
          Expanded(child: ElevatedButton.icon(onPressed: _busy ? null : _printPdf,
              icon: _busy ? const SizedBox(width: 16, height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2))
                  : const Icon(Icons.print_outlined),
              label: const Text('طباعة'))),
        ]),
      ]),
    )),
  );
}

/* ====================== الفواتير المحفوظة ====================== */

class SavedInvoicesScreen extends StatefulWidget {
  const SavedInvoicesScreen({super.key});
  @override
  State<SavedInvoicesScreen> createState() => _SavedInvoicesScreenState();
}

class _SavedInvoicesScreenState extends State<SavedInvoicesScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tab;
  List<Invoice> _all = [];
  bool _loading = true;
  bool _showTrash = false;
  final _searchCtrl = TextEditingController();
  String _query = '';
  DateTime? _fromDate;
  DateTime? _toDate;
  double? _minAmount;
  double? _maxAmount;
  String _statusFilter = 'all';

  @override
  void initState() {
    super.initState();
    _tab = TabController(length: 2, vsync: this);
    _tab.addListener(() => setState(() => _showTrash = _tab.index == 1));
    _load();
  }

  @override
  void dispose() { _tab.dispose(); _searchCtrl.dispose(); super.dispose(); }

  Future<void> _load() async {
    setState(() => _loading = true);
    final list = await InvoiceStore.load();
    if (mounted) setState(() { _all = list; _loading = false; });
  }

  List<Invoice> get _filtered {
    var list = _all.where((i) => _showTrash ? i.isDeleted : !i.isDeleted);
    if (_query.isNotEmpty) {
      final q = _query.toLowerCase();
      list = list.where((i) => i.customer.toLowerCase().contains(q) ||
          i.invoiceNumber.toLowerCase().contains(q));
    }
    if (!_showTrash) {
      if (_statusFilter != 'all') list = list.where((i) => i.status == _statusFilter);
      if (_fromDate != null || _toDate != null) {
        list = list.where((i) {
          try {
            final d = DateFormat(AppConfig.dateFormat, 'ar').parse(i.date);
            if (_fromDate != null && d.isBefore(_fromDate!)) return false;
            if (_toDate != null && d.isAfter(_toDate!)) return false;
            return true;
          } catch (_) { return true; }
        });
      }
      if (_minAmount != null) list = list.where((i) => i.total >= _minAmount!);
      if (_maxAmount != null) list = list.where((i) => i.total <= _maxAmount!);
    }
    return list.toList();
  }

  Future<void> _openFilters() async {
    final fdCtrl = TextEditingController(text: _fromDate != null ? formatDate(_fromDate!) : '');
    final tdCtrl = TextEditingController(text: _toDate != null ? formatDate(_toDate!) : '');
    final minCtrl = TextEditingController(text: _minAmount?.toString() ?? '');
    final maxCtrl = TextEditingController(text: _maxAmount?.toString() ?? '');
    String status = _statusFilter;

    await showModalBottomSheet<bool>(context: context, isScrollControlled: true,
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom,
            left: 16, right: 16, top: 16),
        child: StatefulBuilder(builder: (ctx, setSt) => SingleChildScrollView(
          child: Column(mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('بحث متقدم', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              const Text('حالة الدفع:'), const SizedBox(height: 8),
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(value: 'all', label: Text('الكل')),
                  ButtonSegment(value: 'paid', label: Text('مدفوعة')),
                  ButtonSegment(value: 'unpaid', label: Text('غير مدفوعة')),
                ],
                selected: {status},
                onSelectionChanged: (s) => setSt(() => status = s.first),
              ),
              const SizedBox(height: 16),
              TextField(controller: fdCtrl, readOnly: true,
                decoration: const InputDecoration(labelText: 'من تاريخ',
                    suffixIcon: Icon(Icons.calendar_today)),
                onTap: () async {
                  final d = await showDatePicker(context: ctx,
                      initialDate: _fromDate ?? DateTime.now(),
                      firstDate: DateTime(2020), lastDate: DateTime(2100));
                  if (d != null) { fdCtrl.text = formatDate(d); _fromDate = d; }
                }),
              const SizedBox(height: 12),
              TextField(controller: tdCtrl, readOnly: true,
                decoration: const InputDecoration(labelText: 'إلى تاريخ',
                    suffixIcon: Icon(Icons.calendar_today)),
                onTap: () async {
                  final d = await showDatePicker(context: ctx,
                      initialDate: _toDate ?? DateTime.now(),
                      firstDate: DateTime(2020), lastDate: DateTime(2100));
                  if (d != null) { tdCtrl.text = formatDate(d); _toDate = d; }
                }),
              const SizedBox(height: 12),
              Row(children: [
                Expanded(child: TextField(controller: minCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'أقل مبلغ'))),
                const SizedBox(width: 12),
                Expanded(child: TextField(controller: maxCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'أعلى مبلغ'))),
              ]),
              const SizedBox(height: 20),
              Row(children: [
                Expanded(child: OutlinedButton(onPressed: () {
                  _fromDate = null; _toDate = null; _minAmount = null;
                  _maxAmount = null; _statusFilter = 'all';
                  Navigator.pop(ctx, true);
                }, child: const Text('مسح الفلاتر'))),
                const SizedBox(width: 12),
                Expanded(child: ElevatedButton(onPressed: () {
                  _minAmount = double.tryParse(minCtrl.text.trim());
                  _maxAmount = double.tryParse(maxCtrl.text.trim());
                  _statusFilter = status;
                  Navigator.pop(ctx, true);
                }, child: const Text('تطبيق'))),
              ]),
              const SizedBox(height: 16),
            ]),
        )),
      ));
    fdCtrl.dispose(); tdCtrl.dispose(); minCtrl.dispose(); maxCtrl.dispose();
    if (mounted) setState(() {});
  }

  Future<void> _emptyTrash() async {
    final c = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('إفراغ السلة'),
      content: const Text('سيتم حذف كل الفواتير نهائياً. متأكد؟'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('حذف')),
      ],
    ));
    if (c == true) { await InvoiceStore.emptyTrash(); _load(); }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final list = _filtered;
    final total = list.fold(0.0, (s, i) => s + i.total);

    return Scaffold(
      appBar: AppBar(
        title: const Text('الفواتير المحفوظة'),
        bottom: TabBar(controller: _tab, tabs: const [
          Tab(text: 'النشطة', icon: Icon(Icons.receipt)),
          Tab(text: 'السلة', icon: Icon(Icons.delete_outline)),
        ]),
        actions: [
          if (_showTrash)
            IconButton(tooltip: 'إفراغ', onPressed: _emptyTrash,
                icon: const Icon(Icons.delete_sweep_outlined)),
        ],
      ),
      body: _loading ? const Center(child: CircularProgressIndicator())
          : Column(children: [
              Padding(padding: const EdgeInsets.all(12),
                child: Row(children: [
                  Expanded(child: TextField(controller: _searchCtrl,
                    decoration: const InputDecoration(hintText: 'بحث بالعميل أو الرقم',
                        prefixIcon: Icon(Icons.search)),
                    onChanged: (v) => setState(() => _query = v.trim()))),
                  const SizedBox(width: 8),
                  if (!_showTrash) IconButton.filledTonal(
                    tooltip: 'بحث متقدم', onPressed: _openFilters,
                    icon: const Icon(Icons.tune)),
                ])),
              Expanded(child: list.isEmpty ? Center(
                child: Text(_showTrash ? 'السلة فارغة' : 'لا توجد فواتير مطابقة'))
                : ListView.builder(padding: const EdgeInsets.symmetric(horizontal: 12),
                  itemCount: list.length, itemBuilder: (ctx, i) {
                    final inv = list[list.length - 1 - i];
                    return Card(margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: inv.isPaid ? Colors.green.shade100
                              : t.colorScheme.primaryContainer,
                          child: Icon(inv.isPaid ? Icons.check_circle : Icons.schedule,
                            color: inv.isPaid ? Colors.green.shade800 : t.colorScheme.primary)),
                        title: Text(inv.customer),
                        subtitle: Text('رقم: ${inv.invoiceNumber} • ${inv.date}\n'
                            '${inv.items.length} صنف • ${inv.isPaid ? "مدفوعة" : "غير مدفوعة"}'),
                        isThreeLine: true,
                        trailing: Text(formatCurrency(inv.total), style: TextStyle(
                            fontWeight: FontWeight.bold, color: t.colorScheme.primary)),
                        onTap: () async {
                          await Navigator.push(context, MaterialPageRoute(
                              builder: (_) => InvoiceDetailScreen(invoice: inv)));
                          _load();
                        },
                      ));
                  })),
              if (list.isNotEmpty) Container(
                padding: const EdgeInsets.all(12),
                color: t.colorScheme.primaryContainer,
                child: Row(children: [
                  Text(_showTrash ? 'عدد المحذوفات:' : 'المجموع (${list.length}):',
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                  const Spacer(),
                  Text(formatCurrency(total), style: const TextStyle(
                      fontWeight: FontWeight.bold, fontSize: 16)),
                ])),
            ]),
    );
  }
}

/* ====================== تفاصيل الفاتورة ====================== */

class InvoiceDetailScreen extends StatefulWidget {
  final Invoice invoice;
  const InvoiceDetailScreen({super.key, required this.invoice});
  @override
  State<InvoiceDetailScreen> createState() => _InvoiceDetailScreenState();
}

class _InvoiceDetailScreenState extends State<InvoiceDetailScreen> {
  late Invoice _inv;
  bool _busy = false;

  @override
  void initState() { super.initState(); _inv = widget.invoice; }

  Future<void> _toggleStatus() async {
    final updated = _inv.copyWith(status: _inv.isPaid ? 'unpaid' : 'paid');
    await InvoiceStore.update(updated);
    if (mounted) {
      setState(() => _inv = updated);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(_inv.isPaid ? 'تم وضع علامة مدفوعة' : 'تم وضع علامة غير مدفوعة'),
          behavior: SnackBarBehavior.floating));
    }
  }

  Future<void> _copyInvoice() async {
    final newInv = Invoice(
      id: DateTime.now().microsecondsSinceEpoch.toString(),
      invoiceNumber: generateInvoiceNumber(),
      customer: _inv.customer, date: formatDate(DateTime.now()),
      items: _inv.items.map((e) => e.copy()).toList(),
      discount: _inv.discount, taxRate: _inv.taxRate, status: 'unpaid',
    );
    await InvoiceStore.add(newInv);
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('تم إنشاء نسخة جديدة'), behavior: SnackBarBehavior.floating));
  }

  Future<void> _share() async {
    setState(() => _busy = true);
    try {
      final doc = await buildFullPdf(_inv);
      await Printing.sharePdf(bytes: await doc.save(),
          filename: 'invoice_${_inv.invoiceNumber}.pdf');
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('تعذر: $e')));
    } finally { if (mounted) setState(() => _busy = false); }
  }

  Future<void> _print() async {
    setState(() => _busy = true);
    try {
      final doc = await buildFullPdf(_inv);
      await Printing.layoutPdf(onLayout: (_) => doc.save(),
          name: 'invoice_${_inv.invoiceNumber}.pdf');
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('تعذر: $e')));
    } finally { if (mounted) setState(() => _busy = false); }
  }

  Future<void> _delete() async {
    final c = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('حذف الفاتورة'),
      content: const Text('سيتم نقل الفاتورة إلى السلة. متأكد؟'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('حذف')),
      ],
    ));
    if (c == true) { await InvoiceStore.softDelete(_inv.id); if (mounted) Navigator.pop(context); }
  }

  Future<void> _restore() async {
    await InvoiceStore.restore(_inv.id);
    if (mounted) Navigator.pop(context);
  }

  Future<void> _permanentDelete() async {
    final c = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('حذف نهائي'),
      content: const Text('لن يمكن استعادتها. متأكد؟'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('حذف')),
      ],
    ));
    if (c == true) { await InvoiceStore.permanentDelete(_inv.id); if (mounted) Navigator.pop(context); }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text('فاتورة ${_inv.invoiceNumber}'),
        actions: [
          if (!_inv.isDeleted)
            IconButton(
              tooltip: _inv.isPaid ? 'وضع كغير مدفوعة' : 'وضع كمدفوعة',
              onPressed: _toggleStatus,
              icon: Icon(_inv.isPaid ? Icons.check_circle : Icons.check_circle_outline)),
          PopupMenuButton<String>(onSelected: (v) {
            if (v == 'copy') _copyInvoice();
            if (v == 'delete') _delete();
            if (v == 'restore') _restore();
            if (v == 'permanent') _permanentDelete();
          }, itemBuilder: (ctx) => [
            if (!_inv.isDeleted) const PopupMenuItem(value: 'copy',
                child: ListTile(leading: Icon(Icons.copy), title: Text('نسخ الفاتورة'))),
            if (!_inv.isDeleted) const PopupMenuItem(value: 'delete',
                child: ListTile(leading: Icon(Icons.delete_outline), title: Text('نقل إلى السلة'))),
            if (_inv.isDeleted) const PopupMenuItem(value: 'restore',
                child: ListTile(leading: Icon(Icons.restore), title: Text('استعادة'))),
            if (_inv.isDeleted) const PopupMenuItem(value: 'permanent',
                child: ListTile(leading: Icon(Icons.delete_forever), title: Text('حذف نهائي'))),
          ]),
        ],
      ),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Card(child: Padding(padding: const EdgeInsets.all(16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Expanded(child: Text('العميل: ${_inv.customer}',
                  style: const TextStyle(fontSize: 16))),
              Chip(label: Text(_inv.isPaid ? 'مدفوعة' : 'غير مدفوعة'),
                backgroundColor: _inv.isPaid ? Colors.green.shade100 : Colors.orange.shade100),
            ]),
            const SizedBox(height: 4),
            Text('التاريخ: ${_inv.date}'),
            const SizedBox(height: 4),
            Text('رقم الفاتورة: ${_inv.invoiceNumber}'),
          ]))),
        const SizedBox(height: 16),
        ..._inv.items.map((i) => Card(margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            title: Text(i.name),
            subtitle: Text('${i.qty.toStringAsFixed(2)} × ${formatCurrency(i.price)}'),
            trailing: Text(formatCurrency(i.total),
                style: const TextStyle(fontWeight: FontWeight.bold)),
          ))),
        const SizedBox(height: 16),
        Card(child: Padding(padding: const EdgeInsets.all(16),
          child: Column(children: [
            _r('المجموع الفرعي', _inv.subtotal),
            if (_inv.discount > 0) _r('الخصم', -_inv.discount),
            if (_inv.taxRate > 0)
              _r('الضريبة (${_inv.taxRate.toStringAsFixed(1)}%)', _inv.taxAmount),
            const Divider(),
            _r('الإجمالي', _inv.total, bold: true),
          ]))),
        const SizedBox(height: 24),
        Row(children: [
          Expanded(child: OutlinedButton.icon(onPressed: _busy ? null : _share,
              icon: const Icon(Icons.share_outlined), label: const Text('مشاركة PDF'))),
          const SizedBox(width: 12),
          Expanded(child: ElevatedButton.icon(onPressed: _busy ? null : _print,
              icon: const Icon(Icons.print_outlined), label: const Text('طباعة'))),
        ]),
        const SizedBox(height: 16),
      ]),
    );
  }

  Widget _r(String l, double v, {bool bold = false}) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(children: [
      Text(l, style: TextStyle(fontWeight: bold ? FontWeight.bold : FontWeight.normal)),
      const Spacer(),
      Text(formatCurrency(v), style: TextStyle(
          fontWeight: bold ? FontWeight.bold : FontWeight.normal)),
    ]),
  );
}

/* ====================== دليل السلع والمنتجات ====================== */

class ProductsScreen extends StatefulWidget {
  const ProductsScreen({super.key});
  @override
  State<ProductsScreen> createState() => _ProductsScreenState();
}

class _ProductsScreenState extends State<ProductsScreen> {
  List<QuickItem> _products = [];
  bool _loading = true;
  final _searchCtrl = TextEditingController();
  String _query = '';

  @override
  void initState() { super.initState(); _load(); }

  @override
  void dispose() { _searchCtrl.dispose(); super.dispose(); }

  Future<void> _load() async {
    final list = await ProductStore.load();
    if (mounted) setState(() { _products = list; _loading = false; });
  }

  List<QuickItem> get _filtered {
    if (_query.isEmpty) return _products;
    final q = _query;
    final starts = _products.where((e) => e.name.startsWith(q));
    final contains = _products.where((e) => !e.name.startsWith(q) && e.name.contains(q));
    return [...starts, ...contains];
  }

  void _snack(String m) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(m), behavior: SnackBarBehavior.floating));
  }

  Future<void> _productDialog({QuickItem? initial}) async {
    final n = TextEditingController(text: initial?.name ?? '');
    final p = TextEditingController(text: initial != null ? initial.price.toString() : '');
    final fk = GlobalKey<FormState>();
    final saved = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: Text(initial == null ? 'إضافة سلعة' : 'تعديل سلعة'),
      content: Form(key: fk, child: Column(mainAxisSize: MainAxisSize.min, children: [
        TextFormField(controller: n, autofocus: true,
          decoration: const InputDecoration(labelText: 'اسم السلعة'),
          validator: (v) {
            final name = v?.trim() ?? '';
            if (name.isEmpty) return 'أدخل الاسم';
            final dup = _products.any((e) => e.name.trim() == name &&
                e.name.trim() != (initial?.name.trim() ?? ''));
            return dup ? 'هذا الاسم موجود مسبقاً' : null;
          }),
        const SizedBox(height: 12),
        TextFormField(controller: p,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: const InputDecoration(labelText: 'السعر الافتراضي'),
          validator: (v) {
            final x = double.tryParse(v?.trim() ?? '');
            return (x == null || x < 0) ? 'سعر غير صحيح' : null;
          }),
      ])),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () {
          if (fk.currentState!.validate()) Navigator.pop(ctx, true);
        }, child: const Text('حفظ')),
      ],
    ));
    if (saved == true) {
      if (initial != null && initial.name.trim() != n.text.trim()) {
        await ProductStore.remove(initial.name);
      }
      await ProductStore.upsert(QuickItem(
          name: n.text.trim(), price: double.parse(p.text.trim())));
      await _load();
      _snack(initial == null ? 'تمت إضافة السلعة' : 'تم تعديل السلعة');
    }
    WidgetsBinding.instance.addPostFrameCallback((_) { n.dispose(); p.dispose(); });
  }

  Future<void> _delete(QuickItem item) async {
    final c = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('حذف السلعة'), content: Text('حذف «${item.name}»؟'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('حذف')),
      ],
    ));
    if (c == true) {
      await ProductStore.remove(item.name);
      await _load();
      _snack('تم حذف السلعة');
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final list = _filtered;
    return Scaffold(
      appBar: AppBar(title: const Text('دليل السلع والمنتجات')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _productDialog(),
        icon: const Icon(Icons.add), label: const Text('سلعة جديدة')),
      body: _loading ? const Center(child: CircularProgressIndicator())
          : Column(children: [
              Padding(padding: const EdgeInsets.all(12),
                child: TextField(controller: _searchCtrl,
                  decoration: const InputDecoration(hintText: 'بحث عن سلعة...',
                      prefixIcon: Icon(Icons.search)),
                  onChanged: (v) => setState(() => _query = v.trim()))),
              Expanded(child: list.isEmpty
                ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                    Icon(Icons.inventory_2_outlined, size: 64, color: t.colorScheme.outline),
                    const SizedBox(height: 12),
                    Text(_products.isEmpty ? 'لا توجد سلع بعد — أضف أول سلعة'
                        : 'لا توجد نتائج مطابقة',
                        style: TextStyle(color: t.colorScheme.outline)),
                  ]))
                : ListView.builder(padding: const EdgeInsets.symmetric(horizontal: 12),
                    itemCount: list.length, itemBuilder: (ctx, i) {
                      final item = list[i];
                      return Card(margin: const EdgeInsets.only(bottom: 8),
                        child: ListTile(
                          leading: CircleAvatar(backgroundColor: t.colorScheme.primaryContainer,
                            child: Text(item.name.isNotEmpty ? item.name.substring(0, 1) : '؟')),
                          title: Text(item.name),
                          subtitle: Text(formatCurrency(item.price)),
                          trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                            IconButton(icon: const Icon(Icons.edit_outlined),
                                onPressed: () => _productDialog(initial: item)),
                            IconButton(icon: const Icon(Icons.delete_outline),
                                onPressed: () => _delete(item)),
                          ]),
                        ));
                    })),
            ]),
    );
  }
}

/* ====================== الإعدادات ====================== */

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});
  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late TextEditingController _nameCtrl;
  late TextEditingController _phoneCtrl;
  late TextEditingController _addrCtrl;

  @override
  void initState() {
    super.initState();
    final s = AppSettings.instance;
    _nameCtrl = TextEditingController(text: s.companyName);
    _phoneCtrl = TextEditingController(text: s.companyPhone);
    _addrCtrl = TextEditingController(text: s.companyAddress);
  }

  @override
  void dispose() {
    _nameCtrl.dispose(); _phoneCtrl.dispose(); _addrCtrl.dispose();
    super.dispose();
  }

  Future<void> _saveCompany() async {
    await AppSettings.instance.setCompany(
      name: _nameCtrl.text.trim(), phone: _phoneCtrl.text.trim(),
      address: _addrCtrl.text.trim());
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('تم الحفظ'), behavior: SnackBarBehavior.floating));
  }

  Future<void> _backup() async {
    try {
      final data = await InvoiceStore.export();
      final dir = await getTemporaryDirectory();
      final file = File('${dir.path}/backup_${DateTime.now().millisecondsSinceEpoch}.json');
      await file.writeAsString(data);
      await Share.shareXFiles([XFile(file.path)], subject: 'نسخة احتياطية');
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('تعذر: $e')));
    }
  }

  Future<void> _restore() async {
    final ctrl = TextEditingController();
    final ok = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('استعادة نسخة احتياطية'),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        const Text('الصق محتوى ملف النسخة:'),
        const SizedBox(height: 12),
        TextField(controller: ctrl, maxLines: 6,
            decoration: const InputDecoration(hintText: '{"version":3,...}')),
      ]),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('استعادة')),
      ],
    ));
    if (ok == true) {
      try {
        final n = await InvoiceStore.import(ctrl.text.trim());
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('تم استيراد $n فاتورة')));
      } catch (e) {
        if (mounted) ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('خطأ: $e')));
      }
    }
    ctrl.dispose();
  }

  Future<void> _clearAll() async {
    final c = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('مسح كل البيانات'),
      content: const Text('سيتم حذف كل شيء. لا يمكن التراجع. متأكد؟'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
        ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('حذف الكل')),
      ],
    ));
    if (c == true) {
      await InvoiceStore.clear();
      await QuickItemsStore.save([]);
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('تم مسح كل البيانات')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = AppSettings.instance;
    final t = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('الإعدادات')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        _section('لون التطبيق', Icons.palette_outlined),
        Wrap(spacing: 12, runSpacing: 12,
          children: AppSettings.themeColors.asMap().entries.map((e) {
            final selected = s.colorIndex == e.key;
            return InkWell(onTap: () => s.setColor(e.key),
              borderRadius: BorderRadius.circular(30),
              child: Container(width: 50, height: 50,
                decoration: BoxDecoration(color: e.value, shape: BoxShape.circle,
                  border: Border.all(color: selected ? t.colorScheme.onSurface : Colors.transparent, width: 3)),
                child: selected ? const Icon(Icons.check, color: Colors.white) : null));
          }).toList()),
        const SizedBox(height: 8),
        Text('الحالي: ${AppSettings.themeColorNames[s.colorIndex]}',
            style: TextStyle(color: t.colorScheme.outline)),
        const SizedBox(height: 24),
        _section('المظهر', Icons.brightness_6_outlined),
        SegmentedButton<ThemeMode>(
          segments: const [
            ButtonSegment(value: ThemeMode.light, label: Text('فاتح'), icon: Icon(Icons.light_mode)),
            ButtonSegment(value: ThemeMode.dark, label: Text('داكن'), icon: Icon(Icons.dark_mode)),
            ButtonSegment(value: ThemeMode.system, label: Text('النظام'), icon: Icon(Icons.settings_suggest)),
          ],
          selected: {s.themeMode},
          onSelectionChanged: (v) => s.setMode(v.first)),
        const SizedBox(height: 24),
        _section('الخط العربي', Icons.text_fields),
        ...AppSettings.fontOptions.map((f) => RadioListTile<String>(
          title: Text(AppSettings.fontNamesAr[f] ?? f),
          subtitle: Text(f), value: f, groupValue: s.fontFamily,
          onChanged: (v) { if (v != null) s.setFont(v); })),
        const SizedBox(height: 24),
        _section('بيانات الشركة', Icons.business_outlined),
        TextField(controller: _nameCtrl,
            decoration: const InputDecoration(labelText: 'اسم الشركة')),
        const SizedBox(height: 12),
        TextField(controller: _phoneCtrl,
            decoration: const InputDecoration(labelText: 'رقم الهاتف')),
        const SizedBox(height: 12),
        TextField(controller: _addrCtrl,
            decoration: const InputDecoration(labelText: 'العنوان')),
        const SizedBox(height: 12),
        SizedBox(width: double.infinity, child: ElevatedButton.icon(
            onPressed: _saveCompany, icon: const Icon(Icons.save_outlined),
            label: const Text('حفظ بيانات الشركة'))),
        const SizedBox(height: 24),
        _section('النسخ الاحتياطي', Icons.backup_outlined),
        SizedBox(width: double.infinity, child: OutlinedButton.icon(
            onPressed: _backup, icon: const Icon(Icons.share_outlined),
            label: const Text('تصدير نسخة احتياطية'))),
        const SizedBox(height: 12),
        SizedBox(width: double.infinity, child: OutlinedButton.icon(
            onPressed: _restore, icon: const Icon(Icons.restore),
            label: const Text('استيراد نسخة احتياطية'))),
        const SizedBox(height: 24),
        _section('منطقة الخطر', Icons.warning_amber),
        SizedBox(width: double.infinity, child: OutlinedButton.icon(
            onPressed: _clearAll, icon: const Icon(Icons.delete_forever),
            label: const Text('مسح كل البيانات'),
            style: OutlinedButton.styleFrom(foregroundColor: Colors.red))),
        const SizedBox(height: 24),
      ]),
    );
  }

  Widget _section(String title, IconData icon) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Row(children: [
      Icon(icon), const SizedBox(width: 8),
      Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
    ]),
  );
}

/* ========================= توليد PDF ========================= */

Future<List<pw.Font>> _loadPdfFonts() async {
  final family = AppSettings.instance.fontFamily;
  try {
    switch (family) {
      case 'Tajawal': return [await PdfGoogleFonts.tajawalRegular(), await PdfGoogleFonts.tajawalBold()];
      case 'Amiri': return [await PdfGoogleFonts.amiriRegular(), await PdfGoogleFonts.amiriBold()];
      case 'Almarai': return [await PdfGoogleFonts.almaraiRegular(), await PdfGoogleFonts.almaraiBold()];
      case 'Noto Naskh Arabic': return [await PdfGoogleFonts.notoNaskhArabicRegular(), await PdfGoogleFonts.notoNaskhArabicBold()];
      default: return [await PdfGoogleFonts.cairoRegular(), await PdfGoogleFonts.cairoBold()];
    }
  } catch (e) {
    try { return [await PdfGoogleFonts.amiriRegular(), await PdfGoogleFonts.amiriBold()]; }
    catch (_) { throw Exception('تعذر تحميل الخط.'); }
  }
}

Future<pw.MemoryImage?> _loadLogo() async {
  try {
    final data = await rootBundle.load('assets/logo.png');
    return pw.MemoryImage(data.buffer.asUint8List());
  } catch (_) {
    try {
      final data = await rootBundle.load('assets/icon.png');
      return pw.MemoryImage(data.buffer.asUint8List());
    } catch (_) { return null; }
  }
}

Future<pw.Document> buildFullPdf(Invoice inv) async {
  final fonts = await _loadPdfFonts();
  final regular = fonts[0]; final bold = fonts[1];
  final logo = await _loadLogo();
  final s = AppSettings.instance;

  final doc = pw.Document();
  doc.addPage(pw.MultiPage(
    pageFormat: PdfPageFormat.a4,
    textDirection: pw.TextDirection.rtl,
    theme: pw.ThemeData.withFont(base: regular, bold: bold),
    build: (context) => [
      pw.Row(mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
        crossAxisAlignment: pw.CrossAxisAlignment.start, children: [
        pw.Column(crossAxisAlignment: pw.CrossAxisAlignment.start, children: [
          if (logo != null) ...[pw.Image(logo, width: 60, height: 60), pw.SizedBox(height: 6)],
          pw.Text('فاتورة', style: pw.TextStyle(fontSize: 28,
              fontWeight: pw.FontWeight.bold, color: PdfColors.teal800)),
          pw.SizedBox(height: 4),
          pw.Text('رقم الفاتورة: ${inv.invoiceNumber}'),
          pw.Text('التاريخ: ${inv.date}'),
        ]),
        pw.Column(crossAxisAlignment: pw.CrossAxisAlignment.end, children: [
          pw.Text(s.companyName, style: pw.TextStyle(fontSize: 16, fontWeight: pw.FontWeight.bold)),
          if (s.companyAddress.isNotEmpty) pw.Text(s.companyAddress),
          if (s.companyPhone.isNotEmpty) pw.Text('هاتف: ${s.companyPhone}'),
          pw.SizedBox(height: 4),
          pw.Container(padding: const pw.EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: pw.BoxDecoration(
              color: inv.isPaid ? PdfColors.green100 : PdfColors.orange100,
              borderRadius: pw.BorderRadius.circular(4)),
            child: pw.Text(inv.isPaid ? 'مدفوعة' : 'غير مدفوعة',
                style: pw.TextStyle(fontSize: 10,
                  color: inv.isPaid ? PdfColors.green900 : PdfColors.orange900,
                  fontWeight: pw.FontWeight.bold))),
        ]),
      ]),
      pw.SizedBox(height: 16),
      pw.Divider(color: PdfColors.teal300),
      pw.SizedBox(height: 8),
      pw.Text('العميل: ${inv.customer}', style: const pw.TextStyle(fontSize: 14)),
      pw.SizedBox(height: 16),
      pw.Table(border: pw.TableBorder.all(color: PdfColors.grey400, width: .5), children: [
        pw.TableRow(decoration: const pw.BoxDecoration(color: PdfColors.teal100), children: [
          _c('الصنف', bold: true), _c('الكمية', bold: true),
          _c('السعر', bold: true), _c('الإجمالي', bold: true),
        ]),
        ...inv.items.map((i) => pw.TableRow(children: [
          _c(i.name), _c(i.qty.toStringAsFixed(2)),
          _c(formatCurrency(i.price)), _c(formatCurrency(i.total)),
        ])),
      ]),
      pw.SizedBox(height: 16),
      pw.Container(alignment: pw.Alignment.centerLeft, child: pw.Column(
        crossAxisAlignment: pw.CrossAxisAlignment.end, children: [
        pw.Text('المجموع الفرعي: ${formatCurrency(inv.subtotal)}'),
        if (inv.discount > 0) pw.Text('الخصم: -${formatCurrency(inv.discount)}'),
        if (inv.taxRate > 0) pw.Text('الضريبة (${inv.taxRate.toStringAsFixed(1)}%): ${formatCurrency(inv.taxAmount)}'),
        pw.Divider(),
        pw.Text('الإجمالي: ${formatCurrency(inv.total)}',
            style: pw.TextStyle(fontSize: 16, fontWeight: pw.FontWeight.bold)),
      ])),
      pw.SizedBox(height: 24),
      pw.Center(child: pw.Text('شكرا لتعاملكم معنا',
          style: const pw.TextStyle(fontSize: 11, color: PdfColors.grey700))),
    ],
  ));
  return doc;
}

Future<pw.Document> buildQuickPdf(Invoice inv, {String? notes, double taxAmount = 0}) async {
  final fonts = await _loadPdfFonts();
  final regular = fonts[0]; final bold = fonts[1];
  final logo = await _loadLogo();
  final s = AppSettings.instance;

  final doc = pw.Document();
  doc.addPage(pw.MultiPage(
    pageFormat: PdfPageFormat.a5,
    textDirection: pw.TextDirection.rtl,
    theme: pw.ThemeData.withFont(base: regular, bold: bold),
    build: (context) => [
      pw.Row(mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
        crossAxisAlignment: pw.CrossAxisAlignment.start, children: [
        pw.Column(crossAxisAlignment: pw.CrossAxisAlignment.start, children: [
          if (logo != null) ...[pw.Image(logo, width: 45, height: 45), pw.SizedBox(height: 4)],
          pw.Text(s.companyName, style: pw.TextStyle(fontSize: 13, fontWeight: pw.FontWeight.bold)),
          if (s.companyPhone.isNotEmpty) pw.Text('هاتف: ${s.companyPhone}',
              style: const pw.TextStyle(fontSize: 9)),
        ]),
        pw.Column(crossAxisAlignment: pw.CrossAxisAlignment.end, children: [
          pw.Text('إيصال', style: pw.TextStyle(fontSize: 18,
              fontWeight: pw.FontWeight.bold, color: PdfColors.teal800)),
          pw.SizedBox(height: 4),
          pw.Text('رقم: ${inv.invoiceNumber}', style: const pw.TextStyle(fontSize: 9)),
          pw.Text('التاريخ: ${inv.date}', style: const pw.TextStyle(fontSize: 9)),
        ]),
      ]),
      pw.SizedBox(height: 8),
      pw.Divider(color: PdfColors.grey400),
      pw.SizedBox(height: 4),
      pw.Table(border: pw.TableBorder.all(color: PdfColors.grey300, width: .4), children: [
        pw.TableRow(decoration: const pw.BoxDecoration(color: PdfColors.teal50), children: [
          _c('الصنف', bold: true), _c('الكمية', bold: true),
          _c('السعر', bold: true), _c('الإجمالي', bold: true),
        ]),
        ...inv.items.map((i) => pw.TableRow(children: [
          _c(i.name), _c(i.qty.toStringAsFixed(i.qty % 1 == 0 ? 0 : 2)),
          _c(formatCurrency(i.price)), _c(formatCurrency(i.total)),
        ])),
      ]),
      pw.SizedBox(height: 8),
      pw.Container(alignment: pw.Alignment.centerLeft, child: pw.Column(
        crossAxisAlignment: pw.CrossAxisAlignment.end, children: [
        pw.Text('المجموع: ${formatCurrency(inv.subtotal)}', style: const pw.TextStyle(fontSize: 9)),
        if (inv.discount > 0) pw.Text('خصم: -${formatCurrency(inv.discount)}',
            style: const pw.TextStyle(fontSize: 9)),
        if (taxAmount > 0) pw.Text('ضريبة: +${formatCurrency(taxAmount)}',
            style: const pw.TextStyle(fontSize: 9)),
      ])),
      pw.SizedBox(height: 6),
      pw.Container(padding: const pw.EdgeInsets.all(8),
        decoration: pw.BoxDecoration(color: PdfColors.teal100,
            borderRadius: pw.BorderRadius.circular(6)),
        child: pw.Row(mainAxisAlignment: pw.MainAxisAlignment.spaceBetween, children: [
          pw.Text('الإجمالي', style: pw.TextStyle(fontSize: 13, fontWeight: pw.FontWeight.bold)),
          pw.Text(formatCurrency(inv.total + taxAmount),
              style: pw.TextStyle(fontSize: 17, fontWeight: pw.FontWeight.bold)),
        ])),
      if (notes != null && notes.isNotEmpty) ...[
        pw.SizedBox(height: 10),
        pw.Container(width: double.infinity, padding: const pw.EdgeInsets.all(6),
          decoration: pw.BoxDecoration(color: PdfColors.grey100,
              borderRadius: pw.BorderRadius.circular(4)),
          child: pw.Column(crossAxisAlignment: pw.CrossAxisAlignment.start, children: [
            pw.Text('ملاحظات:', style: pw.TextStyle(fontSize: 9, fontWeight: pw.FontWeight.bold)),
            pw.SizedBox(height: 2),
            pw.Text(notes, style: const pw.TextStyle(fontSize: 9)),
          ])),
      ],
      pw.SizedBox(height: 12),
      pw.Center(child: pw.Text('شكراً لتعاملكم معنا',
          style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey700))),
    ],
  ));
  return doc;
}

pw.Widget _c(String text, {bool bold = false}) => pw.Padding(
  padding: const pw.EdgeInsets.all(5),
  child: pw.Text(text, textAlign: pw.TextAlign.center,
    style: pw.TextStyle(fontSize: 10,
        fontWeight: bold ? pw.FontWeight.bold : pw.FontWeight.normal)),
);
